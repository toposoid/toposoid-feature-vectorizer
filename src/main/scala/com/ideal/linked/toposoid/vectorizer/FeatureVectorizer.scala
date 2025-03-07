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
import com.ideal.linked.toposoid.common.{CLAIM, DOCUMENT_ID, IMAGE, NON_SENTENCE, NonSentenceType, PREMISE, PROPOSITION_ID, REFERENCES, SENTENCE, TABLE_OF_CONTENTS, ToposoidUtils, TransversalState, UNSPECIFIED}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorForUpdate, FeatureVectorIdentifier, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.image.model.SingleImage
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeForImage}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import io.jvm.uuid.UUID

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
  def createVector(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser, transversalState:TransversalState):Unit= Try{

    //Regist Feature Of Sentences
    val featureVectorsPremise:List[FeatureVector] = knowledgeSentenceSetForParser.premiseList.map(x => getSentenceVector(x.knowledge, transversalState))
    val featureVectorsClaim:List[FeatureVector] = knowledgeSentenceSetForParser.claimList.map(x => getSentenceVector(x.knowledge, transversalState))
    createSentenceVectorSub(featureVectorsPremise, knowledgeSentenceSetForParser.premiseList, PREMISE.index, transversalState)
    createSentenceVectorSub(featureVectorsClaim, knowledgeSentenceSetForParser.claimList, CLAIM.index, transversalState)
    //Regist Feature Of Images
    if(knowledgeSentenceSetForParser.premiseList.filter(_.knowledge.knowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.premiseList, PREMISE.index, transversalState)
    }
    if(knowledgeSentenceSetForParser.claimList.filter(_.knowledge.knowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.claimList, CLAIM.index, transversalState)
    }
    //Regist Feature Of Reference (Since the information is linked to the Document, only the Claim is required.)
    val references:List[String] = knowledgeSentenceSetForParser.claimList.foldLeft(List.empty[String]){
      (acc, x) => {
        x.knowledge.documentPageReference.references.size match  {
          case 0 => acc
          case _ => acc ++ x.knowledge.documentPageReference.references
        }
      }
    }
    val documentId = knowledgeSentenceSetForParser.claimList.head.knowledge.knowledgeForDocument.id
    val lang = knowledgeSentenceSetForParser.claimList.head.knowledge.lang
    if (!documentId.equals("") && references.size > 0) {
      references.distinct.foreach( x =>
        createNonSentenceVectorSub(documentId ,x , lang, REFERENCES.index, transversalState)
      )
    }
    //Regist Feature Of TOC (Since the information is linked to the Document, only the Claim is required.)
    val tocs:List[String] = knowledgeSentenceSetForParser.claimList.foldLeft(List.empty[String]) {
      (acc, x) => {
        x.knowledge.documentPageReference.tableOfContents.size match {
          case 0 => acc
          case _ => acc ++ x.knowledge.documentPageReference.tableOfContents
        }
      }
    }
    if (!documentId.equals("") && tocs.size > 0) {
      tocs.distinct.foreach( x =>
        createNonSentenceVectorSub(documentId ,x , lang, TABLE_OF_CONTENTS.index, transversalState)
      )
    }
    logger.debug(ToposoidUtils.formatMessageForLogger("Creating Vector completed.", transversalState.username))
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
  private def createSentenceVectorSub(featureVectors: List[FeatureVector], knowledgeList: List[KnowledgeForParser], sentenceType:Int, transversalState:TransversalState):Unit = Try{

    for ((featureVector, knowledgeForParser) <- (featureVectors zip knowledgeList)) {
      val propositionId: String = knowledgeForParser.propositionId
      val sentenceId: String = knowledgeForParser.sentenceId
      val knowledge: Knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(propositionId, sentenceId, sentenceType, knowledge.lang, PROPOSITION_ID.index, UNSPECIFIED.index)
      val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, featureVector.vector)
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, SENTENCE.index, transversalState)
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
  private def createImageVectorSub(knowledgeForParsers: List[KnowledgeForParser], sentenceType: Int, transversalState:TransversalState): Unit = Try {
    val featureVectorForUpdates: List[FeatureVectorForUpdate] = knowledgeForParsers.foldLeft(List.empty[FeatureVectorForUpdate]) {
      (acc, x) => {
        val partialFeatureVectorForUpdate: List[FeatureVectorForUpdate] = x.knowledge.knowledgeForImages.map(y => {
          val vector = getImageVector(y.imageReference.reference.url, transversalState)
          val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(x.propositionId, y.id, sentenceType, x.knowledge.lang, PROPOSITION_ID.index, UNSPECIFIED.index)
          FeatureVectorForUpdate(featureVectorIdentifier, vector.vector)
        })
        acc ++ partialFeatureVectorForUpdate
      }
    }
    for (featureVectorForUpdate <- featureVectorForUpdates) {
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, IMAGE.index, transversalState)
      if (statusInfo.status == "ERROR") {
        logger.error(statusInfo.message)
        throw new Exception(statusInfo.message)
      }
    }
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  private def createNonSentenceVectorSub(documentId:String, nonSentence:String, lang:String, nonSentenceType: Int, transversalState:TransversalState):Unit = Try {
    val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(documentId, UUID.random.toString, -1, lang, DOCUMENT_ID.index, nonSentenceType)
    val vector = getNonSentenceVector(nonSentence, lang, transversalState)
    val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, vector.vector)
    val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
    val statusInfo = registVector(featureVectorJson, NON_SENTENCE.index, transversalState)
    if (statusInfo.status == "ERROR") {
      logger.error(statusInfo.message)
      throw new Exception(statusInfo.message)
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
  def getSentenceVector(knowledge:Knowledge, transversalState:TransversalState): FeatureVector = Try {
    val langPatternJP: Regex = "^ja_.*".r
    val langPatternEN: Regex = "^en_.*".r

    val commonNLPInfo:(String, String) = knowledge.lang match {
      case langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val json:String = Json.toJson(SingleSentence(sentence=knowledge.sentence)).toString()
    val featureVectorJson:String = ToposoidUtils.callComponent(json, commonNLPInfo._1, commonNLPInfo._2, "getFeatureVector", transversalState)
    logger.debug(ToposoidUtils.formatMessageForLogger("Getting SentenceVector completed.", transversalState.username))
    Json.parse(featureVectorJson).as[FeatureVector]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def getImageVector(imageUrl: String, transversalState:TransversalState): FeatureVector = Try{
    val singleImage = SingleImage(url=imageUrl)
    val json:String = Json.toJson(singleImage).toString()
    val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_HOST"), conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_PORT"), "getFeatureVector", transversalState)
    logger.debug(ToposoidUtils.formatMessageForLogger("Getting ImageVector completed.", transversalState.username))
    Json.parse(featureVectorJson).as[FeatureVector]
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  def getNonSentenceVector(nonSentence: String, lang:String, transversalState: TransversalState): FeatureVector = Try {
    val langPatternJP: Regex = "^ja_.*".r
    val langPatternEN: Regex = "^en_.*".r

    val commonNLPInfo: (String, String) = lang match {
      case langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val json: String = Json.toJson(SingleSentence(sentence = nonSentence)).toString()
    val featureVectorJson: String = ToposoidUtils.callComponent(json, commonNLPInfo._1, commonNLPInfo._2, "getFeatureVector", transversalState)
    logger.debug(ToposoidUtils.formatMessageForLogger("Getting SentenceVector completed.", transversalState.username))
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
  private def registVector(json:String, featureType:Int, transversalState:TransversalState):StatusInfo = Try{

    val statusInfoJson = featureType match  {
      case SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case IMAGE.index =>  ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case NON_SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case _ => """{"status":"ERROR", "message":"BAD CONTENTS"}"""
    }
    Json.parse(statusInfoJson).as[StatusInfo]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def removeVector(knowledgeForParser:KnowledgeForParser, transversalState: TransversalState) = Try{
    //delete sentence vector
    val featureVectorIdentifier:FeatureVectorIdentifier = FeatureVectorIdentifier(knowledgeForParser.propositionId, knowledgeForParser.sentenceId, SENTENCE.index, knowledgeForParser.knowledge.lang, PROPOSITION_ID.index, UNSPECIFIED.index)
    val json = Json.toJson(featureVectorIdentifier).toString()
    deleteVector(json, SENTENCE.index, transversalState)
    //delete image vector
    knowledgeForParser.knowledge.knowledgeForImages.foreach(x => {
      val featureVectorIdentifier:FeatureVectorIdentifier = FeatureVectorIdentifier(knowledgeForParser.propositionId, x.id, IMAGE.index, knowledgeForParser.knowledge.lang, PROPOSITION_ID.index, UNSPECIFIED.index)
      val json = Json.toJson(featureVectorIdentifier).toString()
      deleteVector(json, IMAGE.index, transversalState)
    })
    val documentId = knowledgeForParser.knowledge.knowledgeForDocument.id
    if(!documentId.equals("")){
      val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(knowledgeForParser.knowledge.knowledgeForDocument.id, "-" , NON_SENTENCE.index, knowledgeForParser.knowledge.lang, DOCUMENT_ID.index, REFERENCES.index)
      val json = Json.toJson(featureVectorIdentifier).toString()
      deleteVector(json, NON_SENTENCE.index, transversalState)
    }


  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  private def deleteVector(json: String, featureType: Int, transversalState: TransversalState): StatusInfo = Try {

    val statusInfoJson = featureType match {
      case SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete", transversalState)
      case IMAGE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "delete", transversalState)
      case NON_SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "deleteBySuperiorId", transversalState)
      case _ => """{"status":"ERROR", "message":"BAD CONTENTS"}"""
    }
    Json.parse(statusInfoJson).as[StatusInfo]
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }


}
