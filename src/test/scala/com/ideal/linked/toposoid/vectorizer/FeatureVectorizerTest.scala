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
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, DiagrammedAssertions, FlatSpec}
import io.jvm.uuid.UUID
import play.api.libs.json.Json

class FeatureVectorizerTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll{

  val propositionIdsJp = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val sentenceIdsJp = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val knowledgeForParsersJp = List(
    KnowledgeForParser(propositionIdsJp(0), sentenceIdsJp(0), Knowledge("太郎は映画を見た。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJp(1), sentenceIdsJp(1), Knowledge("太郎は映画を楽しんだ。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJp(2), sentenceIdsJp(2), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false)),
    KnowledgeForParser(propositionIdsJp(3), sentenceIdsJp(3), Knowledge("花子の趣味は庭仕事です。", "ja_JP" ,"{}", false)))

  val propositionIdsEn = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val sentenceIdsEn = List(UUID.random.toString, UUID.random.toString, UUID.random.toString, UUID.random.toString)
  val knowledgeForParsersEn = List(
    KnowledgeForParser(propositionIdsEn(0), sentenceIdsEn(0), Knowledge("Mark went to the doctor.", "en_Us", "{}", false)),
    KnowledgeForParser(propositionIdsEn(1), sentenceIdsEn(1), Knowledge("Mark went to the hospital.", "en_Us", "{}", false)),
    KnowledgeForParser(propositionIdsEn(2), sentenceIdsEn(2), Knowledge("Mary is studying Japanese.", "en_Us", "{}", false)),
    KnowledgeForParser(propositionIdsEn(3), sentenceIdsEn(3),  Knowledge("Mary is interested in Japanese.", "en_Us", "{}", false)))

  val propositionIdsJpEn = List(UUID.random.toString, UUID.random.toString)
  val sentenceIdsJpEn = List(UUID.random.toString, UUID.random.toString)
  val knowledgeForParsersJpEn = List(
    KnowledgeForParser(propositionIdsJpEn(0), sentenceIdsJpEn(0), Knowledge("宇宙は膨張している。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJpEn(1), sentenceIdsJpEn(1), Knowledge("The universe is expanding.", "en_US" ,"{}", false)))


  override def afterAll(): Unit = {

    for(knowledgeForParser <- knowledgeForParsersEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "#" + sentenceId)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }

    for(knowledgeForParser <- knowledgeForParsersJpEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "#" + sentenceId)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }
  }

  "The list of japanese sentences" should "be properly registered in the vald and searchable." in {

    FeatureVectorizer.createVector(knowledgeForParsersJp)
    Thread.sleep(5000)
    for ((knowledgeForParser, i) <- knowledgeForParsersJp.zipWithIndex) {
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
        case 1 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
        case 2 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJp(2) + "#" + knowledge.lang +"#" + sentenceIdsJp(2), propositionIdsJp(3) + "#" + knowledge.lang +"#" + sentenceIdsJp(3)))
        case 3 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJp(2) + "#" + knowledge.lang +"#" + sentenceIdsJp(2), propositionIdsJp(3) + "#" + knowledge.lang +"#" + sentenceIdsJp(3)))
      }
    }
  }


  "The list of English sentences" should "be properly registered in the vald and searchable." in {
    FeatureVectorizer.createVector(knowledgeForParsersEn)
    Thread.sleep(5000)
    for ((knowledgeForParser, i) <- knowledgeForParsersEn.zipWithIndex) {
      val knowledge = knowledgeForParser.knowledge
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsEn(0) + "#" + knowledge.lang +"#" + sentenceIdsEn(0), propositionIdsEn(1) + "#" + knowledge.lang +"#" + sentenceIdsEn(1)))
        case 1 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsEn(0) + "#" + knowledge.lang +"#" + sentenceIdsEn(0), propositionIdsEn(1) + "#" + knowledge.lang +"#" + sentenceIdsEn(1)))
        case 2 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsEn(2) + "#" + knowledge.lang +"#" + sentenceIdsEn(2), propositionIdsEn(3) + "#" + knowledge.lang +"#" + sentenceIdsEn(3)))
        case 3 => assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsEn(2) + "#" + knowledge.lang +"#" + sentenceIdsEn(2), propositionIdsEn(3) + "#" + knowledge.lang +"#" + sentenceIdsEn(3)))
      }
    }
  }


  "The list of japanese and english sentences" should "be properly registered in the vald and searchable." in {
    FeatureVectorizer.createVector(knowledgeForParsersJpEn)
    Thread.sleep(5000)
    for (knowledgeForParser <- knowledgeForParsersJpEn) {
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJpEn(0) + "#ja_JP#" + sentenceIdsJpEn(0), propositionIdsJpEn(1) + "#en_US#" + sentenceIdsJpEn(1)))
    }
  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSentenceSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Eはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Fは黒髪ではない。", "ja_JP", "{}"))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )

    FeatureVectorizer.createVectorForKnowledgeSet(knowledgeSentenceSetForParser)
    Thread.sleep(5000)
    val knowledgeForParsers: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList ::: knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsers.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0) == propositionId + "#" + knowledge.lang +"#" + sentenceId)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsers.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "#" + sentenceId)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }
  }

  "The List of English Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSentenceSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("A's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("B's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("C's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("D's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("E's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("F's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )

    FeatureVectorizer.createVectorForKnowledgeSet(knowledgeSentenceSetForParser)
    Thread.sleep(5000)
    val knowledgeForParsers: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList ::: knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsers.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1, radius = (-1.0f), epsilon = 0.01f, timeout = 50000000000L)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0) == propositionId + "#" + knowledge.lang +"#" + sentenceId)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsers.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorId = FeatureVectorId(id = propositionId + "#" + knowledge.lang + "#" + sentenceId)
      ToposoidUtils.callComponent(Json.toJson(featureVectorId).toString(), conf.getString("TOPOSOID_VALD_ACCESSOR_HOST"), "9010", "delete")
    }


  }

}
