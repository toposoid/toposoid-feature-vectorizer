/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ideal.linked.toposoid.vectorizer

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, SENTENCE, IMAGE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorForUpdate, FeatureVectorIdentifier, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.image.model.SingleImage
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeForImage}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

/**
 * The main implementation of this module is text-to-vector representation conversion.
 * The management of transformed vectors uses VectorDB(weaviate).
 */
object FeatureVectorizer extends LazyLogging {

  /**
   *
   * @param propositionId
   * @param knowledgeSentenceSet
   */
  def createVector(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser):Unit= Try{

    //Regist Feature Of Sentences
    val featureVectorsPremise:List[FeatureVector] = knowledgeSentenceSetForParser.premiseList.map(x => getSentenceVector(x.knowledge))
    val featureVectorsClaim:List[FeatureVector] = knowledgeSentenceSetForParser.claimList.map(x => getSentenceVector(x.knowledge))
    createSentenceVectorSub(featureVectorsPremise, knowledgeSentenceSetForParser.premiseList, PREMISE.index)
    createSentenceVectorSub(featureVectorsClaim, knowledgeSentenceSetForParser.claimList, CLAIM.index)
    //Regist Feature Of Images
    if(knowledgeSentenceSetForParser.premiseList.filter(_.knowledge.KnowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.premiseList, PREMISE.index)
    }
    if(knowledgeSentenceSetForParser.claimList.filter(_.knowledge.KnowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.claimList, CLAIM.index)
    }

  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  /**
   *
   * @param featureVectors
   * @param knowledgeList
   * @param sentenceType
   */
  private def createSentenceVectorSub(featureVectors: List[FeatureVector], knowledgeList: List[KnowledgeForParser], sentenceType:Int):Unit = Try{

    for ((featureVector, knowledgeForParser) <- (featureVectors zip knowledgeList)) {
      val propositionId: String = knowledgeForParser.propositionId
      val sentenceId: String = knowledgeForParser.sentenceId
      val knowledge: Knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(propositionId, sentenceId, sentenceType, knowledge.lang)
      val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, featureVector.vector)
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, SENTENCE.index)
      if (statusInfo.status == "ERROR") {
        logger.error(statusInfo.message)
        throw new Exception(statusInfo.message)
      }
    }

  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param knowledgeForParsers
   * @param sentenceType
   */
  private def createImageVectorSub(knowledgeForParsers: List[KnowledgeForParser], sentenceType: Int): Unit = Try {
    val featureVectorForUpdates: List[FeatureVectorForUpdate] = knowledgeForParsers.foldLeft(List.empty[FeatureVectorForUpdate]) {
      (acc, x) => {
        val partialFeatureVectorForUpdate: List[FeatureVectorForUpdate] = x.knowledge.KnowledgeForImages.map(y => {
          val vector = getImageVector(y)
          val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(x.propositionId, y.id, sentenceType, x.knowledge.lang)
          FeatureVectorForUpdate(featureVectorIdentifier, vector.vector)
        })
        acc ++ partialFeatureVectorForUpdate
      }
    }
    for (featureVectorForUpdate <- featureVectorForUpdates) {
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, IMAGE.index)
      if (statusInfo.status == "ERROR") {
        logger.error(statusInfo.message)
        throw new Exception(statusInfo.message)
      }
    }
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param knowledge
   * @return
   */
  private def getSentenceVector(knowledge:Knowledge): FeatureVector = Try {
    val langPatternJP: Regex = "^ja_.*".r
    val langPatternEN: Regex = "^en_.*".r

    val commonNLPInfo:(String, String) = knowledge.lang match {
      case langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val json:String = Json.toJson(SingleSentence(sentence=knowledge.sentence)).toString()
    val featureVectorJson:String = ToposoidUtils.callComponent(json, commonNLPInfo._1, commonNLPInfo._2, "getFeatureVector")
    Json.parse(featureVectorJson).as[FeatureVector]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  private def getImageVector(knowledgeForImage: KnowledgeForImage): FeatureVector = Try{
    val singleImage = SingleImage(url=knowledgeForImage.imageReference.reference.url)
    val json:String = Json.toJson(singleImage).toString()
    val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_HOST"), conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_PORT"), "getFeatureVector")
    Json.parse(featureVectorJson).as[FeatureVector]
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param json
   * @param lang
   * @return
   */
  private def registVector(json:String, featureType:Int):StatusInfo = Try{

    val statusInfoJson = featureType match  {
      case SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "insert")
      case IMAGE.index =>  ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "insert")
      case _ => """{"status":"ERROR", "message":"BAD CONTENTS"}"""
    }
    Json.parse(statusInfoJson).as[StatusInfo]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }


}
