package com.rockthejvm.blog

import zio.*
import zio.stream.*
import zio.json.*

import java.nio.charset.CharacterCodingException
import scala.util.matching.Regex

object ZStreamExample extends ZIOAppDefault:

  val post1: String = "hello-word.md"
  val post1_content: Array[Byte] =
    """---
      |title: "Hello World"
      |tags: []
      |---
      |======
      |
      |## Generic Heading
      |
      |Even pretend blog posts need a #generic intro.
      |""".stripMargin.getBytes

  val post2: String = "scala-3-extensions.md"
  val post2_content: Array[Byte] =
    """---
      |title: "Scala 3 for You and Me"
      |tags: []
      |---
      |======
      |
      |## Cool Heading
      |
      |This is a post about #Scala and their re-work of #implicits via thing like #extensions.
      |""".stripMargin.getBytes

  val post3: String = "zio-streams.md"
  val post3_content: Array[Byte] =
    """---
      |title: "ZIO Streams: An Introduction"
      |tags: []
      |---
      |======
      |
      |## Some Heading
      |
      |This is a post about #Scala and #ZIO #ZStreams!
  """.stripMargin.getBytes

  val fileMap: Map[String, Array[Byte]] = Map(
    post1 -> post1_content,
    post2 -> post2_content,
    post3 -> post3_content
  )

  // obj 1 - replacing tags in front matter
  val hashFilter: String => Boolean = str =>
    str.startsWith("#") && str.count(_ == '#') == 1 && str.length > 2

  val parseHash: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.filter(hashFilter)

  val punctRegex: Regex = """\p{Punct}""".r

  val removePunctuation: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map(str => punctRegex.replaceAllIn(str, ""))

  val lowercase: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map(_.toLowerCase)

  val collectTagsPipeline: ZPipeline[Any, CharacterCodingException, Byte, String] =
    ZPipeline.utf8Decode >>>
      ZPipeline.splitLines >>>
      ZPipeline.splitOn(" ") >>>
      parseHash >>>
      removePunctuation >>>
      lowercase

  val collectTags: ZSink[Any, Nothing, String, Nothing, Set[String]] =
    ZSink.collectAllToSet

  // obj 2: replaces tags with links

  val addTags: Set[String] => ZPipeline[Any, Nothing, String, String] = tags =>
    ZPipeline.map(contents => contents.replace("tags: []", s"tags: [${tags.mkString(",")}]"))

  val addLinks: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map { line =>
      line.split(" ").map { word =>
        if (hashFilter(word)) {
          s"[$word](/tags/${punctRegex.replaceAllIn(word.toLowerCase, "")})"
        } else {
          word
        }
      }.mkString(" ")
    }

  val addNewLine: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map(line => line + "\n")

  val regeneratePost: Set[String] => ZPipeline[Any, CharacterCodingException, Byte, Byte] = tags =>
    ZPipeline.utf8Decode >>>
      ZPipeline.splitLines >>>
      addTags(tags) >>>
      addLinks >>>
      addNewLine >>>
      ZPipeline.utf8Encode

  def writeFile(dirPath: String, fileName: String): ZSink[Any, Throwable, Byte, Byte, Long] =
    ZSink.fromFileName(dirPath + "/" + fileName)

  def autoTag(fileName: String, contents: Array[Byte]) =
    for
      tags <- ZStream.fromIterable(contents)
        .via(collectTagsPipeline)
        .run(collectTags)
      _ <- Console.printLine(s"Generating file $fileName")
      _ <- ZStream.fromIterable(contents)
        .via(regeneratePost(tags))
        .run(writeFile("src/main/resources/data/zio-streams", fileName))
    yield (fileName, tags)

  val autoTagAll = ZIO.foreach(fileMap) {
    case (fileName, contents) => autoTag(fileName, contents)
  }

  // obj 3
  // Map[fileName, all tags in that file]
  // Map[tag, all files with that tag]

  def createTagIndex(tagMap: Map[String, Set[String]]): ZIO[Any, Throwable, Long] =
    val searchMap = tagMap
      .values
      .toSet
      .flatten
      .map(tag => tag -> tagMap.filter(_._2.contains(tag)).keys.toSet)
      .toMap

    ZStream.fromIterable(searchMap.toJsonPretty.getBytes)
      .run(ZSink.fromFileName("src/main/resources/data/search.json"))

  val parseProgram =
    for
      tagMap <- autoTagAll
      _ <- Console.printLine("Generating index file search.json")
      _ <- createTagIndex(tagMap)
    yield ()

  override def run: ZIO[Any, Any, Any] = parseProgram.debug
