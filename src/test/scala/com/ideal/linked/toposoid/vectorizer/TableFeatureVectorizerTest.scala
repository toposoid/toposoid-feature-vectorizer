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
import com.ideal.linked.toposoid.common.{SentenceType, FeatureType, ToposoidUtils, TransversalState}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.{FeatureVectorSearchResult, SingleFeatureVectorForSearch}
import com.ideal.linked.toposoid.knowledgebase.table.model.{SingleTable, RegisteredTableContentResult}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.FeatureVector
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.Json
import com.ideal.linked.toposoid.knowledgebase.regist.model.TableReference
import org.apache.pekko.http.ccompat.since213
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.knowledgebase.table.model.SingleTable
//import io.jvm.uuid.UUID
import play.api.libs.json.{Json, OWrites, Reads}
import sttp.client4._
import sttp.model._
import scala.concurrent.duration.{Duration, DurationInt}
import com.ideal.linked.toposoid.common.TRANSVERSAL_STATE
import com.ideal.linked.toposoid.vectorizer.FeatureVectorizer.getFeatureVectorSearchResult
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.FeatureVectorIdentifier
import com.ideal.linked.toposoid.common.SuperiorType
import com.ideal.linked.toposoid.common.NonSentenceType
import com.ideal.linked.toposoid.common.CaseGroupType


case class UploadContentContext(featureType: Int, url: String)
object UploadContentContext {
  implicit val jsonWrites: OWrites[UploadContentContext] = Json.writes[UploadContentContext]
  implicit val jsonReads: Reads[UploadContentContext] = Json.reads[UploadContentContext]
}    

case class UploadResult(id: String, url:String, status:Int)
object UploadResult {
  implicit val jsonWrites: OWrites[UploadResult] = Json.writes[UploadResult]
  implicit val jsonReads: Reads[UploadResult] = Json.reads[UploadResult]
}

class TableFeatureVectorizerTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  override def beforeAll(): Unit = {
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
  }

  "The list of japanese sentences" should "be properly registered in the weaviate and searchable and deleted." in {
    //Upload Data
    val originalUrl = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0"    
    val endpoint = "http://" + conf.getString("TOPOSOID_FILE_UPLOAD_FACADE_HOST") + ":" + conf.getString("TOPOSOID_FILE_UPLOAD_FACADE_PORT") + "/upload"    
    val backend = DefaultSyncBackend(
      options = BackendOptions.connectionTimeout(1.minute))
    val request = basicRequest
    .header(TRANSVERSAL_STATE.str, Json.toJson(transversalState).toString())      
    .httpVersion(HttpVersion.HTTP_1_1)
    .post(uri"${endpoint}") // Replace with your upload endpoint
    .multipartBody(
        multipart("featureType", FeatureType.TABLE.index.toString),
        multipart("url", originalUrl), // デフォルト値を明示的に送る場合              
    )
    val response = request.send(backend)
    val responseJson = response.body match {
      case Right(successBody) => s"$successBody"
      case Left(errorBody) => {
        s"Upload failed. Status code: ${response.code}. Error body: $errorBody"
        assert(false)
      }
    }

    val uploadResult = Json.parse(responseJson.toString()).as[UploadResult] 

    //Register Data And Get Data's URL
    val reference: Reference = Reference(url = uploadResult.url,
      surface = "データが",
      surfaceIndex = 0,
      isWholeSentence = false,
      originalUrlOrReference= originalUrl)
    val tableReference: TableReference = TableReference(reference = reference, skipHeaderRows=5, multiHeaderRows=4, sheetNameForExcel="se0101")
    val tableId = java.util.UUID.randomUUID().toString
    val knowledgeForTable: KnowledgeForTable = KnowledgeForTable(id = tableId, tableReference = tableReference)
    val registeredContentResultJson = ToposoidUtils.callComponent(
      Json.toJson(knowledgeForTable).toString(),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_HOST"),
      conf.getString("TOPOSOID_CONTENTS_ADMIN_PORT"),
      "registerTable", transversalState)
    val registeredContentResult: RegisteredTableContentResult = Json.parse(registeredContentResultJson).as[RegisteredTableContentResult]

    val propositionId = java.util.UUID.randomUUID().toString
    val sentenceId = java.util.UUID.randomUUID().toString
    val knowledge:Knowledge = Knowledge(sentence = "データが存在します。", lang = "ja_JP", extentInfoJson = "{}", isNegativeSentence = false, knowledgeForTables = List(registeredContentResult.knowledgeForTable) )
    val knowledgeForParser:KnowledgeForParser = KnowledgeForParser(propositionId, sentenceId, knowledge)
    val knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser( List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])

    //Create Vector
    FeatureVectorizer.createVector(knowledgeSentenceSetForParser, transversalState)

    //Get Collect Table Vector
    val singleTable: SingleTable = SingleTable(url = registeredContentResult.knowledgeForTable.tableReference.reference.url)
    val featureVectorSearchResult = getFeatureVectorSearchResult(FeatureType.TABLE,  "", "ja_JP", Option(singleTable), transversalState)
    //Check
    assert(featureVectorSearchResult.statusInfo.status.equals("OK"))
    assert(featureVectorSearchResult.ids.size == 1)
    assert(featureVectorSearchResult.ids(0).superiorId.equals(propositionId))
    assert(featureVectorSearchResult.ids(0).featureId.equals(tableId))
    assert(featureVectorSearchResult.ids(0).sentenceType == SentenceType.CLAIM.index)
    assert(featureVectorSearchResult.ids(0).lang == "ja_JP")

    //Delete Vector
    knowledgeSentenceSetForParser.claimList.foreach(x => {
      FeatureVectorizer.removeVectorByPropositionId(x, transversalState)
    })
    Thread.sleep(7000)

    val featureVectorIdentifierTABLV = FeatureVectorIdentifier(propositionId, "-", -1, "ja_JP", SuperiorType.PROPOSITION_ID.index, NonSentenceType.UNSPECIFIED.index, CaseGroupType.UNSPECIFIED.index)
    val jsonTABLV: String = Json.toJson(featureVectorIdentifierTABLV).toString()
    val featureVectorSearchResultJsonTABLV: String = ToposoidUtils.callComponent(jsonTABLV, conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_PORT"), "searchBySuperiorId", transversalState)
    val resultTABLV = Json.parse(featureVectorSearchResultJsonTABLV).as[FeatureVectorSearchResult]
    assert(resultTABLV.ids.size == 0)

  }


}
