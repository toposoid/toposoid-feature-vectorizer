# toposoid-feature-vectorizer
The main implementation of this module is text-to-vector representation conversion.
The management of transformed vectors uses [Vald](https://github.com/vdaas/vald).
This library is mainly used by toposoid developer in Toposoid projects.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))


## Requirements
Scala version 2.12.x,   
Sbt version 1.2.8

## Recommended environment
* Required: at least 8GB of RAM
* Required: 10G or higher　of HDD

## Setup
sbt publishLocal

## Usage
Please refer to the test code
```scala
import com.ideal.linked.toposoid.vectorizer.FeatureVectorizer
import io.jvm.uuid.UUID

//Japanese Pattern1
val propositionIdsJp = List(UUID.random.toString, UUID.random.toString)
val knowledgeListJp = List(Knowledge("太郎は映画を見た。", "ja_JP", "{}", false), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false))

FeatureVectorizer.createVector(propositionIdsJp, knowledgeListJp)

//Japanese Pattern2
val propositionIdJp:String = UUID.random.toString
val knowledgeSetJp:KnowledgeSentenceSet = KnowledgeSentenceSet(
  List(Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false),
    Knowledge("Cはブロンドではない。", "ja_JP", "{}", false),
    Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false)),
  List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
  List(Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false),
    Knowledge("Eはブロンドではない。", "ja_JP", "{}", false),
    Knowledge("Fは黒髪ではない。", "ja_JP", "{}")),
  List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
)
FeatureVectorizer.createVectorForKnowledgeSet(propositionIdJp, knowledgeSetJp)

//English Pattern1
val propositionIdsEn = List(UUID.random.toString, UUID.random.toString)
val knowledgeListEn = List(Knowledge("That's life.", "en_US", "{}", false), Knowledge("Seeing is believing.", "en_US" ,"{}", false))
FeatureVectorizer.createVector(propositionIdsEn, knowledgeListEn)

//English Pattern2
val propositionIdEn:String = UUID.random.toString
val knowledgeSetEn: KnowledgeSentenceSet = KnowledgeSentenceSet(
  List(Knowledge("A's hair is not black.", "en_US", "{}", false),
    Knowledge("B's hair is not blonde", "en_US", "{}", false),
    Knowledge("C's hair is not black.", "en_US", "{}", false)),
  List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
  List(Knowledge("D's hair is not black.", "en_US", "{}", false),
    Knowledge("E's hair is not blonde", "en_US", "{}", false),
    Knowledge("F's hair is not black.", "en_US", "{}", false)),
  List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
)
FeatureVectorizer.createVectorForKnowledgeSet(knowledgeSetEn, knowledgeSetEn)
```

## Note

## License
toposoid/toposoid-feature-vectorizer is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!

