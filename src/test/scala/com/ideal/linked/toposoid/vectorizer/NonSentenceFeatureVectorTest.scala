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
import com.ideal.linked.toposoid.common.{ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{DocumentPageReference, Knowledge, KnowledgeForDocument, KnowledgeForImage, KnowledgeForTable, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
//import io.jvm.uuid.UUID
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import play.api.libs.json.Json
class NonSentenceFeatureVectorTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {
  val transversalState: TransversalState = TransversalState(userId = "test-user", username = "guest", roleId = 0, csrfToken = "")

  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
  }

  "The list of japanese sentences" should "be properly registered in the weaviate and searchable and deleted." in {
    val references:List[String] = List(
      "Gyongyosi, L., & Imre, S. (2019). A Survey on quantum computing technology. Computer Science Review, 31, 51-71. https://doi.org/https://doi.org/10.1016/j.cosrev.2018.11.002",
      "de Leon, N. P., Itoh, K. M., Kim, D., Mehta, K. K., Northup, T. E., Paik, H., Palmer, B. S., Samarth, N., Sangtawesin, S., & Steuerman, D. W. (2021). Materials challenges and opportunities for quantum computing hardware. Science, 372(6539), eabb2823. https://doi.org/doi:10.1126/science.abb2823",
      "佐々木達郎(. 2022). 論文・特許クラスター分析を用いた量子コンピュータの学術研究・技術開発動向調査 , SciREX ワー キングペーパー . http://doi.org/10.24545/00001885")
    val tableOfContents:List[String] = List(
      "目次1・・・・・・・・・・・・ p.1",
      "目次2・・・・・・・・・・・・ p.2",
      "目次3・・・・・・・・・・・・ p.3")

    val headlines:List[String] = List(
      "見出し1",
      "見出し2",
      "見出し3"
    )

    val titleOfTopPage = "テストタイトル"
    val documentId = java.util.UUID.randomUUID().toString
    val propositionId = java.util.UUID.randomUUID().toString

    val knowledgeForDocument:KnowledgeForDocument = KnowledgeForDocument(id = documentId, filename = "test.pdf", url = "http://xxx/test.pdf", titleOfTopPage = titleOfTopPage)
    val documentPageReference:DocumentPageReference = DocumentPageReference(pageNo = 1, references = references, tableOfContents = tableOfContents, headlines=headlines)
    val knowledge = Knowledge(sentence = "非文のテストです。", lang = "ja_JP", extentInfoJson = "{}", knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference)

    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId = propositionId, sentenceId = java.util.UUID.randomUUID().toString, knowledge = knowledge)
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      premiseList = List.empty[KnowledgeForParser],
      premiseLogicRelation = List.empty[PropositionRelation],
      claimList = List(knowledgeForParser),
      claimLogicRelation = List.empty[PropositionRelation])

    //Create Vector
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser, transversalState)

    //Search Vector
    val referencesSearchJsonList = references.map(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      //Check
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 1)
      searchJson
    })

    val tableOfContentsSearchJsonList = tableOfContents.map(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      //Check
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 1)
      searchJson
    })

    val headlinesSearchJsonList = headlines.map(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      //Check
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 1)
      searchJson
    })

    val titleOfTopPageJsonList = List(titleOfTopPage).map(x => {
      val json: String = Json.toJson(SingleSentence(sentence = x)).toString()
      val featureVectorJson: String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), "9006", "getFeatureVector", transversalState)
      val vector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]
      val searchOb = SingleFeatureVectorForSearch(vector = vector.vector, num = 10)
      val searchJson = Json.toJson(searchOb).toString()
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      //Check
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 1)
      searchJson
    })

    //Delete Vector
    knowledgeSentenceSetForParser.claimList.foreach(x => {
      FeatureVectorizer.removeAllVectorByDocumentId(documentId, List(propositionId), transversalState)
    })
    Thread.sleep(7000)
    //Check

    referencesSearchJsonList.map(x => {
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(x, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 0)
    })

    tableOfContentsSearchJsonList.map(x => {
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(x, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 0)
    })

    headlinesSearchJsonList.map(x => {
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(x, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 0)
    })

    titleOfTopPageJsonList.map(x => {
      val featureVectorSearchResultJson = ToposoidUtils.callComponent(x, conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_NON_SENTENCE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
      val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]
      assert(featureVectorSearchResult.similarities.filter(_ > 0.99).size == 0)
    })

  }

}
