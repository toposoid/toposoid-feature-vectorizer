# toposoid-feature-vectorizer
The main implementation of this module is text-to-vector representation conversion and image-to-vector representation conversion.
The management of transformed vectors uses [Weaviate](https://github.com/weaviate/weaviate).
This library is mainly used by toposoid developer in Toposoid projects.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))


## Requirements
Scala version 2.13.x,   
Sbt version 1.9.0

The following microservices must be running
* toposoid/toposoid-common-nlp-japanese-web
* toposoid/toposoid-common-nlp-english-web
* toposoid-common-image-recognition-web
* toposoid/data-accessor-weaviate-web
* semitechnologies/weaviate

## Recommended environment For Standalone
* Required: at least 16GB of RAM
* Required: 30G or higher　of HDD

## Setup For Standalone
sbt publishLocal

## Usage For Standalone
Please refer to the test code
```scala
package com.ideal.linked.toposoid.vectorizer
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.vectorizer.FeatureVectorizer
import io.jvm.uuid.UUID

object JapaneseTest extends App {
  //Japanese Sentence
  val knowledge1 = KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("太郎は映画を見た。", "ja_JP", "{}", false))
  val knowledge2 = KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("花子は美容院に行った。", "ja_JP", "{}", false))

  val knowledgeSentenceSetForParser1 = KnowledgeSentenceSetForParser(
    List.empty[KnowledgeForParser],
    List.empty[PropositionRelation],
    List(knowledge1, knowledge2),
    List (PropositionRelation("AND", 0, 1)))
  FeatureVectorizer.createVector(knowledgeSentenceSetForParser1)
}  
object EnglishTest extends App {
  //English Simple Sentence
  val knowledge1 = KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("That's life.", "en_US", "{}", false)))
  val knowledge2 = KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("If you can dream it, you can do it.", "en_US", "{}", false)))

  val knowledgeSentenceSetForParser1 = KnowledgeSentenceSetForParser(
    List.empty[KnowledgeForParser],
    List.empty[PropositionRelation],
    List(knowledge1,knowledge2),
    List(PropositionRelation("AND", 0, 1)))
  FeatureVectorizer.createVector(knowledgeSentenceSetForParser1)
}
```
* Image vectorization can be done by setting KnowledgeForImages.

## Note

## License
This program is offered under a commercial and under the AGPL license.
For commercial licensing, contact us at https://toposoid.com/contact.  For AGPL licensing, see below.

AGPL licensing:
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!

