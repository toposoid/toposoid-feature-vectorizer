/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ideal.linked.toposoid.vectorizer

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, FeatureType, NonSentenceType, CaseGroupType, SuperiorType, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorForUpdate, FeatureVectorIdentifier, FeatureVectorSearchResult, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.image.model.SingleImage
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeForImage}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
//import io.jvm.uuid.UUID

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.SingleFeatureVectorForSearch
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionEdge
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseNode
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionNode
import com.ideal.linked.toposoid.common.DeductionUtilsForSemiGlobal
import com.ideal.linked.toposoid.protocol.model.base.MatchedKnowledgeNode
import com.ideal.linked.toposoid.protocol.model.base.MatchedFeatureInfo

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
    createSentenceVectorSub(featureVectorsPremise, knowledgeSentenceSetForParser.premiseList, SentenceType.PREMISE.index, transversalState)
    createSentenceVectorSub(featureVectorsClaim, knowledgeSentenceSetForParser.claimList, SentenceType.CLAIM.index, transversalState)
    //Regist Feature Of Images
    if(knowledgeSentenceSetForParser.premiseList.filter(_.knowledge.knowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.premiseList, SentenceType.PREMISE.index, transversalState)
    }
    if(knowledgeSentenceSetForParser.claimList.filter(_.knowledge.knowledgeForImages.size > 0).size > 0) {
      createImageVectorSub(knowledgeSentenceSetForParser.claimList, SentenceType.CLAIM.index, transversalState)
    }

    nonSentenceVectorFacade(knowledgeSentenceSetForParser, NonSentenceType.REFERENCES, transversalState:TransversalState)
    nonSentenceVectorFacade(knowledgeSentenceSetForParser, NonSentenceType.TABLE_OF_CONTENTS, transversalState:TransversalState)
    nonSentenceVectorFacade(knowledgeSentenceSetForParser, NonSentenceType.HEADLINES, transversalState:TransversalState)
    nonSentenceVectorFacade(knowledgeSentenceSetForParser, NonSentenceType.TITLE_OF_TOP_PAGE, transversalState:TransversalState)

    /*
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
    */
    logger.debug(ToposoidUtils.formatMessageForLogger("Creating Vector completed.", transversalState.username))
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  private def nonSentenceVectorFacade(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser, nonSentenceType:NonSentenceType, transversalState:TransversalState) :Unit = {
    val nonSentences: List[String] = knowledgeSentenceSetForParser.claimList.foldLeft(List.empty[String]) {
      (acc, x) => {
        val nonSentences:List[String] = nonSentenceType match   {
          case NonSentenceType.REFERENCES =>  x.knowledge.documentPageReference.references
          case NonSentenceType.TABLE_OF_CONTENTS => x.knowledge.documentPageReference.tableOfContents
          case NonSentenceType.HEADLINES => x.knowledge.documentPageReference.headlines
          case NonSentenceType.TITLE_OF_TOP_PAGE => List(x.knowledge.knowledgeForDocument.titleOfTopPage)
          case _ => List.empty[String]
        }
        nonSentences.size match {
          case 0 => acc
          case _ => acc ++ nonSentences
        }
      }
    }
    val documentId = knowledgeSentenceSetForParser.claimList.head.knowledge.knowledgeForDocument.id
    val lang = knowledgeSentenceSetForParser.claimList.head.knowledge.lang
    if (!documentId.equals("") && nonSentences.size > 0) {
      nonSentences.distinct.foreach(x =>
        createNonSentenceVectorSub(documentId, x, lang, NonSentenceType.REFERENCES.index, transversalState)
      )
    }
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
      val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(propositionId, sentenceId, sentenceType, knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, featureVector.vector)
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, FeatureType.SENTENCE.index, transversalState)
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
          val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(x.propositionId, y.id, sentenceType, x.knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
          FeatureVectorForUpdate(featureVectorIdentifier, vector.vector)
        })
        acc ++ partialFeatureVectorForUpdate
      }
    }
    for (featureVectorForUpdate <- featureVectorForUpdates) {
      val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
      val statusInfo = registVector(featureVectorJson, FeatureType.IMAGE.index, transversalState)
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
    val featureVectorIdentifier: FeatureVectorIdentifier = FeatureVectorIdentifier(documentId, java.util.UUID.randomUUID().toString, -1, lang, SuperiorType.DOCUMENT_ID.index, nonSentenceType, CaseGroupType.UNSPECIFIED.index)
    val vector = getNonSentenceVector(nonSentence, lang, transversalState)
    val featureVectorForUpdate = FeatureVectorForUpdate(featureVectorIdentifier, vector.vector)
    val featureVectorJson = Json.toJson(featureVectorForUpdate).toString()
    val statusInfo = registVector(featureVectorJson, FeatureType.NON_SENTENCE.index, transversalState)
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
    //val langPatternJP: Regex = "^ja_.*".r
    //val langPatternEN: Regex = "^en_.*".r

    //Special Symbol is embedded in English
    val commonNLPInfo:(String, String) = knowledge.lang match {
      case ToposoidUtils.langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case ToposoidUtils.langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case ToposoidUtils.langPatternSpecialSymbol1() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
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
    //val langPatternJP: Regex = "^ja_.*".r
    //val langPatternEN: Regex = "^en_.*".r

    //Special Symbol is embedded in English. But Can there be special symbols in NonSentence?
    val commonNLPInfo: (String, String) = lang match {
      case ToposoidUtils.langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case ToposoidUtils.langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case ToposoidUtils.langPatternSpecialSymbol1() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
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
      case FeatureType.SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case FeatureType.IMAGE.index =>  ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case FeatureType.NON_SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "insert", transversalState)
      case _ => """{"status":"ERROR", "message":"BAD CONTENTS"}"""
    }
    Json.parse(statusInfoJson).as[StatusInfo]
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def removeVectorByPropositionId(knowledgeForParser:KnowledgeForParser, transversalState: TransversalState) = Try{
    //delete sentence vector
    val featureVectorIdentifier:FeatureVectorIdentifier = FeatureVectorIdentifier(knowledgeForParser.propositionId, knowledgeForParser.sentenceId, FeatureType.SENTENCE.index, knowledgeForParser.knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
    val json = Json.toJson(featureVectorIdentifier).toString()
    deleteVector(json, FeatureType.SENTENCE.index, transversalState)
    //delete image vector
    knowledgeForParser.knowledge.knowledgeForImages.foreach(x => {
      val featureVectorIdentifier:FeatureVectorIdentifier = FeatureVectorIdentifier(knowledgeForParser.propositionId, x.id, FeatureType.IMAGE.index, knowledgeForParser.knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      val json = Json.toJson(featureVectorIdentifier).toString()
      deleteVector(json, FeatureType.IMAGE.index, transversalState)
    })
    //TODO:delete table vector
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def removeAllVectorByDocumentId(documentId:String, propositionIds:List[String], transversalState: TransversalState): Unit = {
    propositionIds.foreach(propositionId => {
      this.removeAllVectorBySuperiorId(
        conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"),
        conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"),
        propositionId, transversalState
      )
      this.removeAllVectorBySuperiorId(
        conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"),
        conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"),
        propositionId, transversalState
      )
      //TODO:delete table vector
    })
    this.removeAllVectorBySuperiorId(
      conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"),
      conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"),
      documentId, transversalState
    )
  }
  private def removeAllVectorBySuperiorId(host:String, port:String, superiorId:String, transversalState: TransversalState) = Try{
    //Other than superiorId, as long as there is no validation error, it's fine.
    val featureVectorIdentifier2:FeatureVectorIdentifier = FeatureVectorIdentifier(superiorId, featureId = "-", sentenceType = 1, lang = "ja_JP", superiorType = 0, nonSentenceType = 0, caseGroupType = 0)
    ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier2).toString(), host, port, "deleteBySuperiorId", transversalState)
  }

  private def deleteVector(json: String, featureType: Int, transversalState: TransversalState): StatusInfo = Try {
    val statusInfoJson = featureType match {
      case FeatureType.SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete", transversalState)
      case FeatureType.IMAGE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "delete", transversalState)
      case FeatureType.NON_SENTENCE.index => ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "deleteBySuperiorId", transversalState)
      case _ => """{"status":"ERROR", "message":"BAD CONTENTS"}"""
    }
    Json.parse(statusInfoJson).as[StatusInfo]
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def getFeatureVectorSearchResult(featureType:FeatureType,  sentence:String, lang:String, url:String, transversalState:TransversalState): FeatureVectorSearchResult = {

    val featureVectorSearchResultJson = featureType match {
      case FeatureType.SENTENCE => {
        val vector = getSentenceVector(Knowledge(sentence, lang, "{}"), transversalState)
        val json: String = Json.toJson(SingleFeatureVectorForSearch(vector = vector.vector, num = conf.getString("TOPOSOID_SENTENCE_VECTORDB_SEARCH_NUM_MAX").toInt)).toString()
        ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      }
      case FeatureType.IMAGE => {
        val vector = getImageVector(url, transversalState)
        val json: String = Json.toJson(SingleFeatureVectorForSearch(vector = vector.vector, num = conf.getString("TOPOSOID_IMAGE_VECTORDB_SEARCH_NUM_MAX").toInt)).toString()
        ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      }
      case FeatureType.TABLE => {
        //TODO:Implement
        Json.toJson(FeatureVectorSearchResult(List.empty[FeatureVectorIdentifier], List.empty[Float], StatusInfo("Ok", ""))).toString
      } 
      case _ => {
        Json.toJson(FeatureVectorSearchResult(List.empty[FeatureVectorIdentifier], List.empty[Float], StatusInfo("Ok", ""))).toString
      }
    }

    Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]

  }
  /*
  def getMatchedSentenceFeature(aso:AnalyzedSentenceObject, transversalState:TransversalState): List[CoveredPropositionEdge] = {

    val originalSentenceId = aso.knowledgeBaseSemiGlobalNode.sentenceId
    val originalSentenceType = aso.knowledgeBaseSemiGlobalNode.sentenceType
    val sentence = aso.knowledgeBaseSemiGlobalNode.sentence
    val lang = aso.knowledgeBaseSemiGlobalNode.localContextForFeature.lang

    val vector = getSentenceVector(Knowledge(sentence, lang, "{}"), transversalState)
    val json: String = Json.toJson(SingleFeatureVectorForSearch(vector = vector.vector, num = conf.getString("TOPOSOID_SENTENCE_VECTORDB_SEARCH_NUM_MAX").toInt)).toString()
    val featureVectorSearchResultJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
    val result = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]

    //VecotrDBにClaimとして存在している場合に推論が可能になる
    val (ids, similarities) = (result.ids zip result.similarities).foldLeft((List.empty[FeatureVectorIdentifier], List.empty[Float])) {
      (acc, x) => {
        x._1.sentenceType match {
          case SentenceType.CLAIM.index => (acc._1 :+ x._1, acc._2 :+ x._2)
          case _ => acc
        }
      }
    }

    val filteredResult = FeatureVectorSearchResult(ids, similarities, result.statusInfo) 
    val deductionUnitName = conf.getString("TOPOSOID_DEDUCTION_UNIT_NAME")
    filteredResult.ids.size match {
      case 0 => List.empty[CoveredPropositionEdge]
      case _ => {        
        val featureVectorSearchInfoList = DeductionUtilsForSemiGlobal.extractExistInNeo4JResultForSentence(filteredResult, originalSentenceType, transversalState)        
        val matchedKnowledgeNodes = featureVectorSearchInfoList.map(x => {
          MatchedKnowledgeNode(
            propositionId = x.propositionId,
            sentenceId = x.sentenceId,
            nodeId = "",
            caseNameOnEdge = "",
            isDenialWord = false,
            nodeType = x.sentenceType,
            featureInfo = MatchedFeatureInfo(featureId = x.featureId, similarity = x.similarity)
          )          
        })

        aso.edgeList.map(x => {
          val sourceNode = aso.nodeMap.get(x.sourceId).get.asInstanceOf[KnowledgeBaseNode]
          val destinationNode = aso.nodeMap.get(x.destinationId).get.asInstanceOf[KnowledgeBaseNode]
          val sourceCoveredPropositionNode = CoveredPropositionNode(
            terminalId = sourceNode.nodeId,
            terminalSurface = sourceNode.predicateArgumentStructure.surface,
            terminalUrl = "",
            matchedKnowledgeNodes = matchedKnowledgeNodes,
            isConfirmed = true,
            deductionUnit = deductionUnitName
          )

          val destinationCoveredPropositionNode = CoveredPropositionNode(
            terminalId = destinationNode.nodeId,
            terminalSurface = destinationNode.predicateArgumentStructure.surface,
            terminalUrl = "",
            matchedKnowledgeNodes = matchedKnowledgeNodes,
            isConfirmed = true,
            deductionUnit = deductionUnitName
          )
          CoveredPropositionEdge(sourceCoveredPropositionNode, destinationCoveredPropositionNode)
        }) 
      }
    }    
        
  }
  */
}
