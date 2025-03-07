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
import com.ideal.linked.toposoid.common.{ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{FeatureVector, SingleSentence}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{DocumentPageReference, Knowledge, KnowledgeForDocument, KnowledgeForImage, KnowledgeForTable, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import io.jvm.uuid.UUID
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
    val knowledgeForDocument:KnowledgeForDocument = KnowledgeForDocument(id = UUID.random.toString, filename = "test.pdf", url = "http://xxx/test.pdf", titleOfTopPage = "テストタイトル")
    val documentPageReference:DocumentPageReference = DocumentPageReference(pageNo = 1, references = references, tableOfContents = tableOfContents)
    val knowledge = Knowledge(sentence = "目次リファレンスのテストです。", lang = "ja_JP", extentInfoJson = "{}", knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference)

    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId = UUID.random.toString, sentenceId = UUID.random.toString, knowledge = knowledge)
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

    //Delete Vector
    knowledgeSentenceSetForParser.claimList.foreach(x => {
      FeatureVectorizer.removeVector(x, transversalState)
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


  }

}
