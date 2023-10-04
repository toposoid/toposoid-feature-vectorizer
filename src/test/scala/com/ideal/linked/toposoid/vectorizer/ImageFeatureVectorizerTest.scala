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

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, RegistContentResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.image.model.SingleImage
import com.ideal.linked.toposoid.knowledgebase.nlp.model.FeatureVector
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.Json
import io.jvm.uuid.UUID


class ImageFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema")
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "createSchema")
  }

  "The list of japanese sentences" should "be properly registered in the vald and searchable." in {
    //Regist Image And Get Image's URL
    val reference: Reference = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg",
      surface = "猫が",
      surfaceIndex = 0,
      isWholeSentence = false)
    val imageReference: ImageReference = ImageReference(reference = reference, 27, 41, 287, 435)
    val imageId = UUID.random.toString
    val knowledgeForImage: KnowledgeForImage = KnowledgeForImage(id = imageId, imageReference = imageReference)
    val registContentResultJson = ToposoidUtils.callComponent(
      Json.toJson(knowledgeForImage).toString(),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_HOST"),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_PORT"),
      "registImage")
    val registContentResult: RegistContentResult = Json.parse(registContentResultJson).as[RegistContentResult]

    val propositionId = UUID.random.toString
    val sentenceId = UUID.random.toString
    val knowledge:Knowledge = Knowledge(sentence = "猫が２匹います。", lang = "ja_JP", extentInfoJson = "{}", isNegativeSentence = false, KnowledgeForImages = List(knowledgeForImage))
    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId, sentenceId, knowledge)
    val knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser( List.empty[KnowledgeForParser],
                                                                                                      List.empty[PropositionRelation],
                                                                                                      List(knowledgeForParser),
                                                                                                      List.empty[PropositionRelation])
    //Create Vector
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser)

    //Get Collect Image Vector
    val singleImage: SingleImage = SingleImage(registContentResult.url)
    val featureVectorJson: String = ToposoidUtils.callComponent(
      Json.toJson(singleImage).toString(),
      conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_HOST"),
      conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_PORT"),
      "getFeatureVector")
    val featureVector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]

    //Search Vector
    val searchOb = SingleFeatureVectorForSearch(vector = featureVector.vector, num = 10)
    val searchJson = Json.toJson(searchOb).toString()
    val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "search")
    val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]

    //Check
    assert(featureVectorSearchResult.statusInfo.status.equals("OK"))
    assert(featureVectorSearchResult.ids.size == 1)
    assert(featureVectorSearchResult.ids(0).propositionId.equals(propositionId))
    assert(featureVectorSearchResult.ids(0).featureId.equals(imageId))
    assert(featureVectorSearchResult.ids(0).sentenceType == CLAIM.index)
    assert(featureVectorSearchResult.ids(0).lang == "ja_JP")

  }

}
