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
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorId, FeatureVectorIdentifier, FeatureVectorSearchResult, SingleFeatureVectorForSearch, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import io.jvm.uuid.UUID
import play.api.libs.json.Json

class SentenceFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{
  
  val propositionIdsJp = List(TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID())
  val sentenceIdsJp = List(TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID())
  val knowledgeForParsersJp = List(
    KnowledgeForParser(propositionIdsJp(0), sentenceIdsJp(0), Knowledge("太郎は映画を見た。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJp(1), sentenceIdsJp(1), Knowledge("太郎は映画を楽しんだ。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJp(2), sentenceIdsJp(2), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false)),
    KnowledgeForParser(propositionIdsJp(3), sentenceIdsJp(3), Knowledge("花子の趣味は庭仕事です。", "ja_JP" ,"{}", false)))

  val propositionIdsEn = List(TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID())
  val sentenceIdsEn = List(TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID(), TestUtils.getUUID())
  val knowledgeForParsersEn = List(
    KnowledgeForParser(propositionIdsEn(0), sentenceIdsEn(0), Knowledge("Mark went to the doctor.", "en_US", "{}", false)),
    KnowledgeForParser(propositionIdsEn(1), sentenceIdsEn(1), Knowledge("Mark went to the hospital.", "en_US", "{}", false)),
    KnowledgeForParser(propositionIdsEn(2), sentenceIdsEn(2), Knowledge("Mary is studying Japanese.", "en_US", "{}", false)),
    KnowledgeForParser(propositionIdsEn(3), sentenceIdsEn(3),  Knowledge("Mary is interested in Japanese.", "en_US", "{}", false)))

  val propositionIdsJpEn = List(TestUtils.getUUID(), TestUtils.getUUID())
  val sentenceIdsJpEn = List(TestUtils.getUUID(), TestUtils.getUUID())
  val knowledgeForParsersJpEn = List(
    KnowledgeForParser(propositionIdsJpEn(0), sentenceIdsJpEn(0), Knowledge("宇宙は膨張している。", "ja_JP", "{}", false)),
    KnowledgeForParser(propositionIdsJpEn(1), sentenceIdsJpEn(1), Knowledge("The universe is expanding.", "en_US" ,"{}", false)))


  def registSingleClaim(knowledgeForParser:KnowledgeForParser): Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser)
    Thread.sleep(7000)
  }

  before {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema")
  }

  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema")
  }

  override def afterAll(): Unit = {

    for (knowledgeForParser <- knowledgeForParsersJp) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = CLAIM.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }

    for(knowledgeForParser <- knowledgeForParsersEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = CLAIM.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }

    for(knowledgeForParser <- knowledgeForParsersJpEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = CLAIM.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }
  }

  "The list of japanese sentences" should "be properly registered in the vald and searchable." in {

    knowledgeForParsersJp.map(registSingleClaim(_))
    for ((knowledgeForParser, i) <- knowledgeForParsersJp.zipWithIndex) {
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => {
          //Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsJp(0), propositionIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(0), sentenceIdsJp(1)))
        }
        case 1 => {
          //Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsJp(0), propositionIdsJp(1) ))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(0), sentenceIdsJp(1)))
        }
        case 2 => {
          //Set(propositionIdsJp(2) + "#" + knowledge.lang +"#" + sentenceIdsJp(2), propositionIdsJp(3) + "#" + knowledge.lang +"#" + sentenceIdsJp(3)))
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsJp(2), propositionIdsJp(3) ))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(2), sentenceIdsJp(3)))
        }
        case 3 => {
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsJp(2), propositionIdsJp(3)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(2), sentenceIdsJp(3)))
        }
      }
    }
  }

  "The list of English sentences" should "be properly registered in the vald and searchable." in {
    knowledgeForParsersEn.map(registSingleClaim(_))
    //FeatureVectorizer.createVector(knowledgeForParsersEn)
    //Thread.sleep(5000)
    for ((knowledgeForParser, i) <- knowledgeForParsersEn.zipWithIndex) {
      val knowledge = knowledgeForParser.knowledge
      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => {
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsEn(0), propositionIdsEn(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(0), sentenceIdsEn(1)))
        }
        case 1 => {
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsEn(0), propositionIdsEn(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(0), sentenceIdsEn(1)))
        }
        case 2 => {
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsEn(2), propositionIdsEn(3)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(2), sentenceIdsEn(3)))
        }
        case 3 => {
          assert(featureVectorSearchResult.ids.map(_.propositionId).toSet == Set(propositionIdsEn(2), propositionIdsEn(3)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(2), sentenceIdsEn(3)))
        }
      }
    }
  }


  "The list of japanese and english sentences" should "be properly registered in the vald and searchable." in {
    //FeatureVectorizer.createVector(knowledgeForParsersJpEn)
    //Thread.sleep(5000)
    knowledgeForParsersJpEn.map(registSingleClaim(_))
    for (knowledgeForParser <- knowledgeForParsersJpEn) {
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      val jpResult = featureVectorSearchResult.ids.filter(_.lang.equals("ja_JP"))
      val enResult = featureVectorSearchResult.ids.filter(_.lang.equals("en_US"))
      assert(jpResult.map(_.propositionId).toSet == Set(propositionIdsJpEn(0)))
      assert(jpResult.map(_.featureId).toSet == Set(sentenceIdsJpEn(0)))
      assert(enResult.map(_.propositionId).toSet == Set(propositionIdsJpEn(1)))
      assert(enResult.map(_.featureId).toSet == Set(sentenceIdsJpEn(1)))
      //assert(featureVectorSearchResult.ids.toSet == Set(propositionIdsJpEn(0) + "#ja_JP#" + sentenceIdsJpEn(0), propositionIdsJpEn(1) + "#en_US#" + sentenceIdsJpEn(1)))
    }
  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = TestUtils.getUUID()
    val knowledgeSentenceSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Eはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("Fは黒髪ではない。", "ja_JP", "{}"))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )

    FeatureVectorizer.createVector(knowledgeSentenceSetForParser)
    Thread.sleep(7000)
    //val knowledgeForParsers: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList ::: knowledgeSentenceSetForParser.claimList
    val knowledgeForParsersPremise: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList
    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).propositionId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == PREMISE.index)
    }

    val knowledgeForParsersClaim: List[KnowledgeForParser] = knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).propositionId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == CLAIM.index)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = PREMISE.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = CLAIM.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }
  }

  "The List of English Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = TestUtils.getUUID()
    val knowledgeSentenceSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("A's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("B's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("C's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("D's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("E's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, TestUtils.getUUID(), Knowledge("F's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )

    FeatureVectorizer.createVector(knowledgeSentenceSetForParser)
    Thread.sleep(7000)
    val knowledgeForParsersPremise: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList
    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).propositionId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == PREMISE.index)
    }

    val knowledgeForParsersClaim: List[KnowledgeForParser] = knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector")
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search")
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).propositionId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == CLAIM.index)
    }


    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = PREMISE.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(propositionId = propositionId, featureId = sentenceId, sentenceType = CLAIM.index, lang = knowledge.lang)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete")
    }

  }

}
