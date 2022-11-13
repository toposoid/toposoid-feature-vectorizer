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
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorId, FeatureVectorSearchResult, SingleFeatureVectorForSearch, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, DiagrammedAssertions, FlatSpec}
import io.jvm.uuid.UUID
import play.api.libs.json.Json

class FeatureVectorizerTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll{

  val propositionIdsJp = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val knowledgeListJp = List(Knowledge("太郎は映画を見た。", "ja_JP", "{}", false), Knowledge("太郎は映画を楽しんだ。", "ja_JP", "{}", false), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false), Knowledge("花子の趣味は庭仕事です。", "ja_JP" ,"{}", false))

  val propositionIdsEn = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val knowledgeListEn = List(Knowledge("Mark went to the doctor.", "en_Us", "{}", false), Knowledge("Mark went to the hospital.", "en_Us", "{}", false), Knowledge("Mary is studying Japanese.", "en_Us", "{}", false), Knowledge("Mary is interested in Japanese.", "en_Us", "{}", false))

  val propositionIdsJpEn = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val knowledgeListJpEn = List(Knowledge("宇宙は膨張している。", "ja_JP", "{}", false), Knowledge("The universe is expanding.", "en_US" ,"{}", false))

  override def afterAll(): Unit = {
    for((knowledgeInfo, i) <- (propositionIdsJp zip knowledgeListJp).zipWithIndex){
      val id = knowledgeInfo._1
      val knowledge = knowledgeInfo._2
      val featureVectorId = FeatureVectorId(id = id + "#" + knowledge.lang + "-s" + i.toString)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }

    for((knowledgeInfo, i) <- (propositionIdsEn zip knowledgeListEn).zipWithIndex){
      val id = knowledgeInfo._1
      val knowledge = knowledgeInfo._2
      val featureVectorId = FeatureVectorId(id = id + "#" + knowledge.lang + "-s" + i.toString)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }

    for((knowledgeInfo, i) <- (propositionIdsJpEn zip knowledgeListJpEn).zipWithIndex){
      val id = knowledgeInfo._1
      val knowledge = knowledgeInfo._2
      val featureVectorId = FeatureVectorId(id = id + "#" + knowledge.lang + "-s" + i.toString)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }
  }

  "The list of japanese sentences" should "be properly registered in the vald and searchable." in {

    FeatureVectorizer.createVector(propositionIdsJp, knowledgeListJp)
    Thread.sleep(90000)
    for ((knowledge, i) <- knowledgeListJp.zipWithIndex) {
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      assert(featureVectorSearchResult.ids(0).startsWith(propositionIdsJp(i)))
    }
  }


  "The list of English sentences" should "be properly registered in the vald and searchable." in {
    FeatureVectorizer.createVector(propositionIdsEn, knowledgeListEn)
    Thread.sleep(90000)
    for ((knowledge, i) <- knowledgeListEn.zipWithIndex) {
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      assert(featureVectorSearchResult.ids(0).startsWith(propositionIdsEn(i)))
    }
  }


  "The list of japanese and english sentences" should "be properly registered in the vald and searchable." in {
    FeatureVectorizer.createVector(propositionIdsJpEn, knowledgeListJpEn)
    Thread.sleep(90000)
    for ((knowledge, i) <- knowledgeListJpEn.zipWithIndex) {
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      assert(featureVectorSearchResult.ids(0).startsWith(propositionIdsJpEn(i)))
    }
  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeSet: KnowledgeSentenceSet = KnowledgeSentenceSet(
      List(Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Cはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false)),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Eはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Fは黒髪ではない。", "ja_JP", "{}")),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )
    val propositionId = UUID.random.toString
    FeatureVectorizer.createVectorForKnowledgeSet(propositionId, knowledgeSet)
    Thread.sleep(90000)
    val knowledgeList: List[Knowledge] = knowledgeSet.premiseList ::: knowledgeSet.claimList
    for ((knowledge, i) <- knowledgeList.zipWithIndex) {
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).startsWith(propositionId))
    }

    for ((knowledge, i) <- knowledgeList.zipWithIndex) {
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "-s" + i.toString)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }
  }

  "The List of English Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeSet: KnowledgeSentenceSet = KnowledgeSentenceSet(
      List(Knowledge("A's hair is not black.", "en_US", "{}", false),
        Knowledge("B's hair is not blonde", "en_US", "{}", false),
        Knowledge("C's hair is not black.", "en_US", "{}", false)),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(Knowledge("D's hair is not black.", "en_US", "{}", false),
        Knowledge("E's hair is not blonde", "en_US", "{}", false),
        Knowledge("F's hair is not black.", "en_US", "{}", false)),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )
    val propositionId = UUID.random.toString
    FeatureVectorizer.createVectorForKnowledgeSet(propositionId, knowledgeSet)
    Thread.sleep(90000)
    val knowledgeList: List[Knowledge] = knowledgeSet.premiseList ::: knowledgeSet.claimList
    for ((knowledge, i) <- knowledgeList.zipWithIndex) {
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).startsWith(propositionId))
    }

    for ((knowledge, i) <- knowledgeList.zipWithIndex) {
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "-s" + i.toString)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }


  }

}
