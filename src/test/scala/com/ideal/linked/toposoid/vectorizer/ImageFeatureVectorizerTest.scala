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
import com.ideal.linked.toposoid.common.{SentenceType, ToposoidUtils, TransversalState}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.image.model.{SingleImage, RegisteredImageContentResult}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.FeatureVector
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.Json
import com.ideal.linked.toposoid.vectorizer.FeatureVectorizer.getFeatureVectorSearchResult
import com.ideal.linked.toposoid.common.FeatureType
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.FeatureVectorIdentifier
import com.ideal.linked.toposoid.common.SuperiorType
import com.ideal.linked.toposoid.common.NonSentenceType
import com.ideal.linked.toposoid.common.CaseGroupType
//import io.jvm.uuid.UUID


class ImageFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
  }

  "The list of japanese sentences" should "be properly registered in the weaviate and searchable and deleted." in {
    //Regist Image And Get Image's URL
    val reference: Reference = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg",
      surface = "猫が",
      surfaceIndex = 0,
      isWholeSentence = false,
      originalUrlOrReference= "http://images.cocodataset.org/val2017/000000039769.jpg")
    val imageReference: ImageReference = ImageReference(reference = reference, 27, 41, 287, 435)
    val imageId = java.util.UUID.randomUUID().toString
    val knowledgeForImage: KnowledgeForImage = KnowledgeForImage(id = imageId, imageReference = imageReference)
    val registeredContentResultJson = ToposoidUtils.callComponent(
      Json.toJson(knowledgeForImage).toString(),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_HOST"),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_PORT"),
      "registerImage", transversalState)
    val registeredContentResult: RegisteredImageContentResult = Json.parse(registeredContentResultJson).as[RegisteredImageContentResult]

    val propositionId = java.util.UUID.randomUUID().toString
    val sentenceId = java.util.UUID.randomUUID().toString
    val knowledge:Knowledge = Knowledge(sentence = "猫が２匹います。", lang = "ja_JP", extentInfoJson = "{}", isNegativeSentence = false, knowledgeForImages = List(registeredContentResult.knowledgeForImage))
    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId, sentenceId, knowledge)
    val knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser( List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])

    //Create Vector
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser, transversalState)

    //Get Collect Image Vector
    val singleImage: SingleImage = SingleImage(registeredContentResult.knowledgeForImage.imageReference.reference.url)
    val featureVectorSearchResult = getFeatureVectorSearchResult(FeatureType.IMAGE,  "", "ja_JP", Option(singleImage), transversalState)

    //Check
    assert(featureVectorSearchResult.statusInfo.status.equals("OK"))
    assert(featureVectorSearchResult.ids.size == 1)
    assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
    assert(featureVectorSearchResult.ids(0).featureId.equals(imageId))
    assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.CLAIM.index)
    assert(featureVectorSearchResult.ids(0).lang == "ja_JP")

    //Delete Vector
    knowledgeSentenceSetForParser.claimList.foreach(x => {
      FeatureVectorizer.removeVectorByPropositionId(x, transversalState)
    })
    Thread.sleep(7000)

    val featureVectorIdentifierIMGV = FeatureVectorIdentifier(propositionId, "-", -1, "ja_JP", SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
    val jsonIMGV: String = Json.toJson(featureVectorIdentifierIMGV).toString()
    val featureVectorSearchResultJsonIMGV: String = ToposoidUtils.callComponent(jsonIMGV, conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "searchBySuperiorId", transversalState)
    val checkIMGV = Json.parse(featureVectorSearchResultJsonIMGV).as[FeatureVectorSearchResult]
    assert(checkIMGV.ids.size == 0)
  }


}
