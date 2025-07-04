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

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils, TransversalState}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, RegistContentResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.image.model.SingleImage
import com.ideal.linked.toposoid.knowledgebase.nlp.model.FeatureVector
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.Json
import io.jvm.uuid.UUID


class ImageFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
  }

  "The list of japanese sentences" should "be properly registered in the weaviate and searchable and deleted." in {
    //Regist Image And Get Image's URL
    val reference: Reference = Reference(url = "",
      surface = "猫が",
      surfaceIndex = 0,
      isWholeSentence = false,
      originalUrlOrReference= "http://images.cocodataset.org/val2017/000000039769.jpg")
    val imageReference: ImageReference = ImageReference(reference = reference, 27, 41, 287, 435)
    val imageId = UUID.random.toString
    val knowledgeForImage: KnowledgeForImage = KnowledgeForImage(id = imageId, imageReference = imageReference)
    val registContentResultJson = ToposoidUtils.callComponent(
      Json.toJson(knowledgeForImage).toString(),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_HOST"),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_PORT"),
      "registImage", transversalState)
    val registContentResult: RegistContentResult = Json.parse(registContentResultJson).as[RegistContentResult]

    val propositionId = UUID.random.toString
    val sentenceId = UUID.random.toString
    val knowledge:Knowledge = Knowledge(sentence = "猫が２匹います。", lang = "ja_JP", extentInfoJson = "{}", isNegativeSentence = false, knowledgeForImages = List(registContentResult.knowledgeForImage))
    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId, sentenceId, knowledge)
    val knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser( List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])

    //Create Vector
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser, transversalState)

    //Get Collect Image Vector
    val singleImage: SingleImage = SingleImage(registContentResult.knowledgeForImage.imageReference.reference.url)
    val featureVectorJson: String = ToposoidUtils.callComponent(
      Json.toJson(singleImage).toString(),
      conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_HOST"),
      conf.getString("TOPOSOID_COMMON_IMAGE_RECOGNITION_PORT"),
      "getFeatureVector", transversalState)
    val featureVector: FeatureVector = Json.parse(featureVectorJson).as[FeatureVector]

    //Search Vector
    val searchOb = SingleFeatureVectorForSearch(vector = featureVector.vector, num = 10)
    val searchJson = Json.toJson(searchOb).toString()
    val featureVectorSearchResultJson = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
    val featureVectorSearchResult: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson).as[FeatureVectorSearchResult]

    //Check
    assert(featureVectorSearchResult.statusInfo.status.equals("OK"))
    assert(featureVectorSearchResult.ids.size == 1)
    assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
    assert(featureVectorSearchResult.ids(0).featureId.equals(imageId))
    assert(featureVectorSearchResult.ids(0).sentenceType == CLAIM.index)
    assert(featureVectorSearchResult.ids(0).lang == "ja_JP")

    //Delete Vector
    knowledgeSentenceSetForParser.claimList.foreach(x => {
      FeatureVectorizer.removeVector(x, transversalState)
    })
    Thread.sleep(7000)
    val featureVectorSearchResultJson2 = ToposoidUtils.callComponent(searchJson, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "search", transversalState)
    val featureVectorSearchResult2: FeatureVectorSearchResult = Json.parse(featureVectorSearchResultJson2).as[FeatureVectorSearchResult]
    //Check
    assert(featureVectorSearchResult2.ids.size == 0)

  }


}
