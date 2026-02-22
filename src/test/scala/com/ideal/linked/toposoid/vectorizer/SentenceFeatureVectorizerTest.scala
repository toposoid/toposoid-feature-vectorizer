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
import com.ideal.linked.toposoid.common.{SentenceType, SuperiorType, NonSentenceType, CaseGroupType, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorId, FeatureVectorIdentifier, FeatureVectorSearchResult, SingleFeatureVectorForSearch, StatusInfo}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
//import io.jvm.uuid.UUID
import play.api.libs.json.Json

class SentenceFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")

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
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser, transversalState)
    Thread.sleep(7000)
  }

  before {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema",transversalState)
  }

  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema",transversalState)
  }

  override def afterAll(): Unit = {

    for (knowledgeForParser <- knowledgeForParsersJp) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.CLAIM.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }

    for(knowledgeForParser <- knowledgeForParsersEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.CLAIM.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }

    for(knowledgeForParser <- knowledgeForParsersJpEn){
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge
      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.CLAIM.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }
  }

  "The list of japanese sentences" should "be properly registered in the vald and searchable." in {

    knowledgeForParsersJp.map(registSingleClaim(_))
    for ((knowledgeForParser, i) <- knowledgeForParsersJp.zipWithIndex) {
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => {
          //Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsJp(0), propositionIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(0), sentenceIdsJp(1)))
        }
        case 1 => {
          //Set(propositionIdsJp(0) + "#" + knowledge.lang +"#" + sentenceIdsJp(0), propositionIdsJp(1) + "#" + knowledge.lang +"#" + sentenceIdsJp(1)))
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsJp(0), propositionIdsJp(1) ))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(0), sentenceIdsJp(1)))
        }
        case 2 => {
          //Set(propositionIdsJp(2) + "#" + knowledge.lang +"#" + sentenceIdsJp(2), propositionIdsJp(3) + "#" + knowledge.lang +"#" + sentenceIdsJp(3)))
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsJp(2), propositionIdsJp(3) ))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsJp(2), sentenceIdsJp(3)))
        }
        case 3 => {
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsJp(2), propositionIdsJp(3)))
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
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      i match {
        case 0 => {
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsEn(0), propositionIdsEn(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(0), sentenceIdsEn(1)))
        }
        case 1 => {
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsEn(0), propositionIdsEn(1)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(0), sentenceIdsEn(1)))
        }
        case 2 => {
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsEn(2), propositionIdsEn(3)))
          assert(featureVectorSearchResult.ids.map(_.featureId).toSet == Set(sentenceIdsEn(2), sentenceIdsEn(3)))
        }
        case 3 => {
          assert(featureVectorSearchResult.ids.map(_.superiorId).toSet == Set(propositionIdsEn(2), propositionIdsEn(3)))
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
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 2)
      val jpResult = featureVectorSearchResult.ids.filter(_.lang.equals("ja_JP"))
      val enResult = featureVectorSearchResult.ids.filter(_.lang.equals("en_US"))
      assert(jpResult.map(_.superiorId).toSet == Set(propositionIdsJpEn(0)))
      assert(jpResult.map(_.featureId).toSet == Set(sentenceIdsJpEn(0)))
      assert(enResult.map(_.superiorId).toSet == Set(propositionIdsJpEn(1)))
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

    FeatureVectorizer.createVector(knowledgeSentenceSetForParser,transversalState)
    Thread.sleep(7000)
    //val knowledgeForParsers: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList ::: knowledgeSentenceSetForParser.claimList
    val knowledgeForParsersPremise: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList
    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.PREMISE.index)
    }

    val knowledgeForParsersClaim: List[KnowledgeForParser] = knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.CLAIM.index)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.PREMISE.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.CLAIM.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
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

    FeatureVectorizer.createVector(knowledgeSentenceSetForParser,transversalState)
    Thread.sleep(7000)
    val knowledgeForParsersPremise: List[KnowledgeForParser] = knowledgeSentenceSetForParser.premiseList
    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.PREMISE.index)
    }

    val knowledgeForParsersClaim: List[KnowledgeForParser] = knowledgeSentenceSetForParser.claimList
    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {

      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val json: String = Json.toJson(SingleSentence(sentence = knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector",transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 1)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search",transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 1)
      assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
      assert(featureVectorSearchResult.ids(0).featureId.equals(sentenceId))
      assert(featureVectorSearchResult.ids(0).lang.equals(knowledge.lang))
      assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.CLAIM.index)
    }


    for ((knowledgeForParser, i) <- knowledgeForParsersPremise.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.PREMISE.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }

    for ((knowledgeForParser, i) <- knowledgeForParsersClaim.zipWithIndex) {
      val propositionId = knowledgeForParser.propositionId
      val sentenceId = knowledgeForParser.sentenceId
      val knowledge = knowledgeForParser.knowledge

      val featureVectorIdentifier = FeatureVectorIdentifier(superiorId = propositionId, featureId = sentenceId, sentenceType = SentenceType.CLAIM.index, lang = knowledge.lang, SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
      ToposoidUtils.callComponent(Json.toJson(featureVectorIdentifier).toString(), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "delete",transversalState)
    }

  }

  "The list of English sentences" should "be properly registered and deleted." in {
    knowledgeForParsersEn.foreach(x => {
      //registSingleClaim(x)
      FeatureVectorizer.removeVectorByPropositionId(x, transversalState)
    })
    knowledgeForParsersEn.foreach(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x.knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), "9008", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 0)
    })
  }


  "The list of japanese sentences" should "be properly registered and deleted." in {
    knowledgeForParsersJp.foreach(x => {
      //registSingleClaim(x)
      FeatureVectorizer.removeVectorByPropositionId(x, transversalState)
    })
    knowledgeForParsersJp.foreach(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x.knowledge.sentence)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.ids.size == 0)
    })
  }


}
