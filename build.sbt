name := "hello-finagle"

version := "1.0"

organization := "org.mama"

scalaVersion := "2.9.1"

resolvers += "twitter.com" at "http://maven.twttr.com/"

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

resolvers += "lucene.apache.org" at "http://lucene.apache.org/"

libraryDependencies ++= Seq(
      "org.apache.lucene" % "lucene-core" % "3.6.0",
      "com.twitter" % "finagle-core" % "5.3.0",
      "com.twitter" % "finagle-http" % "5.3.0",
      "com.twitter" % "finagle-stream" % "5.3.0",
      "com.twitter" % "finagle-redis" % "5.3.0",
      "com.codahale" % "jerkson_2.9.1" % "0.5.0",
      "com.twitter" % "joauth" % "1.9.3")
