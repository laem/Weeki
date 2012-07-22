package com.twitter.finagle.example.stream

import java.net._
import java.util.UUID
import com.twitter.conversions.time._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.util._
import java.nio.charset.Charset
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.stream.Stream
import com.twitter.joauth._
import com.twitter.finagle.ServiceFactory
import com.twitter.finagle.stream.StreamResponse

case class OAuthCredentials(token: String, consumerKey: String, tokenSecret: String, consumerSecret: String)

class OAuthStreamingClient(hostname: String, port: Int, creds: OAuthCredentials, path: String) {
  val scheme = "https"
  val address = new InetSocketAddress(hostname, port)

  val clientFactory = ClientBuilder()
    .codec(new Stream)
    .tls(hostname)
    .hosts(Seq(address))
    .hostConnectionLimit(1)
    .buildFactory()

  val signatureMethod = OAuthParams.HMAC_SHA1
  val realm = "https://www.twitter.com"
  val oauthVersion = OAuthParams.ONE_DOT_OH

  val oauthStr = """OAuth realm="%s",
                   oauth_consumer_key="%s",
                   oauth_token="%s",
                   oauth_signature_method="%s",
                   oauth_signature="%s",
                   oauth_timestamp="%s",
                   oauth_nonce="%s",
                   oauth_version="%s"
  """.replaceAll(",\\s+",",")

  def buildHeader() = {
    val timestampSecs = Time.now.inSeconds
    val timestampStr = "" + timestampSecs
    val nonce = UUID.randomUUID().toString()
    val oauthParams = OAuth1Params(creds.token, creds.consumerKey, nonce, timestampSecs, timestampStr,
                                   null, signatureMethod, oauthVersion)
    val normStr = StandardNormalizer(scheme, hostname, port, "GET", path, List(), oauthParams)
    val sig = Signer()(normStr, creds.tokenSecret, creds.consumerSecret)
    oauthStr.format(realm, creds.consumerKey, creds.token, signatureMethod, URLEncoder.encode(sig), timestampSecs, nonce, oauthVersion).trim
  }

  def go {

    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
    request.setHeader("Authorization", buildHeader())
    request.setHeader("User-Agent", "Finagle 4.0.2 - Liftweb")
    request.setHeader("Host", hostname)

    val client = clientFactory.apply()()
    val streamResponse = client(request)
    streamResponse.onSuccess {
      streamResponse => {
        var messageCount = 0 // Wait for 1000 messages then shut down.
        val startTime= Time.now.inMilliseconds
        streamResponse.messages foreach {
          buffer =>
            messageCount += 1
            println(messageCount)
            println(buffer.toString(Charset.defaultCharset))
            if (messageCount == 1000) {
              
              client.release()
              //clientFactory.close()
            }
            // We return a Future indicating when we've completed processing the message.
            Future.Done
        }
      }
    }

  }
}