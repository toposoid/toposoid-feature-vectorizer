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
import com.ideal.linked.toposoid.common.ToposoidUtils
import com.ideal.linked.toposoid.common.{PREMISE, CLAIM}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorForUpdate, FeatureVectorIdentifier, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

/**
 * The main implementation of this module is text-to-vector representation conversion.
 * The management of transformed vectors uses Vald.
 */
object FeatureVectorizer extends LazyLogging {

  /**
   *
   * @param propositionId
   * @param knowledgeSentenceSet
   */
  def createVector(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser):Unit= Try{

    val featureVectorsPremise:List[FeatureVector] = knowledgeSentenceSetForParser.premiseList.map(x => getVector(x.knowledge))
    val featureVectorsClaim:List[FeatureVector] = knowledgeSentenceSetForParser.claimList.map(x => getVector(x.knowledge))
    createVectorSub(featureVectorsPremise, knowledgeSentenceSetForParser.premiseList, PREMISE.index)
    createVectorSub(featureVectorsClaim, knowledgeSentenceSetForParser.claimList, CLAIM.index)

  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  private def createVectorSub(featureVectors: List[FeatureVector], knowledgeList: List[KnowledgeForParser], sentenceType:Int):Unit = Try{

    for ((featureVector, knowledgeForParser) <- (featureVectors zip knowledgeList)) {
      val propositionId: String = knowledgeForParser.propositionId
      val sentenceId: String = knowledgeForParser.sentenceId
      val knowledge: Knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(propositionId, sentenceId, sentenceType, knowledge.lang)
      val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, featureVector.vector)
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, knowledge.lang)
      if (statusInfo.status == "ERROR") {
        logger.error(statusInfo.message)
        throw new Exception(statusInfo.message)
      }
    }

    print("check")

  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param knowledge
   * @return
   */
  def getVector(knowledge:Knowledge): FeatureVector = Try {
    val langPatternJP: Regex = "^ja_.*".r
    val langPatternEN: Regex = "^en_.*".r

    val commonNLPInfo:(String, String) = knowledge.lang match {
      case langPatternJP() => (conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006")
      case langPatternEN() => (conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008")
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val json:String = Json.toJson(SingleSentence(sentence=knowledge.sentence)).toString()
    val featureVectorJson:String = ToposoidUtils.callComponent(json, commonNLPInfo._1, commonNLPInfo._2, "getFeatureVector")
    Json.parse(featureVectorJson).as[FeatureVector]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param json
   * @param lang
   * @return
   */
  private def registVector(json:String, lang:String):StatusInfo = Try{
    val statusInfoJson = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "upsert")
    Json.parse(statusInfoJson).as[StatusInfo]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   *
   * @param propositionIds
   * @param knowledgeList
   */
  /*
  @deprecated
  def createVector(knowledgeForParserList:List[KnowledgeForParser]):Unit=  Try{
    for (knowledgeForParser <- knowledgeForParserList) {
      val propositionId: String = knowledgeForParser.propositionId
      val sentenceId:String = knowledgeForParser.sentenceId
      val knowledge: Knowledge = knowledgeForParser.knowledge
      val featureVector: FeatureVector = getVector(knowledge)
      val featureVectorForUpdate = FeatureVectorForUpdate(id = propositionId +  "#" + knowledge.lang + "#" + sentenceId , vector = featureVector.vector)
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, knowledge.lang)
      if (statusInfo.status == "ERROR") {
        logger.error(statusInfo.message)
        throw new Exception(statusInfo.message)
      }
    }
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }
  */

}
