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
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorForUpdate, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet}
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
   * @param propositionIds
   * @param knowledgeList
   */
  def createVector(propositionIds:List[String], knowledgeList:List[Knowledge]):Unit=  Try{
    for ((knowledgeInfo, sentenceId) <- (propositionIds zip knowledgeList).zipWithIndex) {
      val propositionId: String = knowledgeInfo._1
      val knowledge: Knowledge = knowledgeInfo._2
      val featureVector: FeatureVector = getVector(knowledge)
      val featureVectorForUpdate = FeatureVectorForUpdate(id = propositionId +  "#" + knowledge.lang + "-s" + sentenceId.toString , vector = featureVector.vector)
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

  /**
   *
   * @param propositionId
   * @param knowledgeSentenceSet
   */
  def createVectorForKnowledgeSet(propositionId:String, knowledgeSentenceSet:KnowledgeSentenceSet):Unit= Try{

    val featureVectorsPremise:List[FeatureVector] = knowledgeSentenceSet.premiseList.map(getVector(_))
    val featureVectorsClaim:List[FeatureVector] = knowledgeSentenceSet.claimList.map(getVector(_))
    for ((knowledgeVecInfo, sentenceId) <- (featureVectorsPremise:::featureVectorsClaim zip knowledgeSentenceSet.premiseList ::: knowledgeSentenceSet.claimList).zipWithIndex) {
      val featureVector = knowledgeVecInfo._1
      val knowledge = knowledgeVecInfo._2
      val featureVectorForUpdate = FeatureVectorForUpdate(id = propositionId + "#" + knowledge.lang +"-s" + sentenceId.toString, vector = featureVector.vector)
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


}
