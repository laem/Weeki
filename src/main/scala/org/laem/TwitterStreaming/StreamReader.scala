package org.laem.TwitterStreaming

import com.twitter.joauth.{StandardSigner, StandardNormalizer, OAuth1Params, OAuthParams}
import com.twitter.util.{Future, Time}
import com.twitter.conversions.time._
import java.util
import java.net.URLEncoder
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod, HttpVersion, DefaultHttpRequest}
import com.twitter.finagle.ServiceFactory
import com.twitter.finagle.stream.{Stream, StreamResponse}
import com.twitter.finagle.builder.ClientBuilder
import org.jboss.netty.util.CharsetUtil
import com.twitter.joauth.OAuth1Params
import com.twitter.joauth.Signer
import com.codahale.jerkson.Json._
import com.codahale.jerkson.ParsingException
import com.cybozu.labs.langdetect.DetectorFactory
import com.twitter.finagle.example.stream.OAuthCredentials


/**
 * Finagle retrieving tweets from the Twitter stream api (the sample api)
 */
object StreamReader {

  val token = "205177680-KBCTbOkyid4X8Tleq6avNsfvebvrTap1hWpmFH7g"
  val consumerKey = "8xYq5KHY2FYwB2OoPSEdeg"
  val tokenSecret = "Jlg6cpwduL1sQwc3ALpFay4nQnbCkwBtdHDPDNw0"
  val consumerSecret = "wCmZkmAEZFLxkYXht1DJXwN2c4B8S3TadCxPjMkSRlc"

  val host = "stream.twitter.com"
  val port = 443
  val hostAndPort = host + ":" + port
  val path = "/1/statuses/sample.json"
  val creds: OAuthCredentials = OAuthCredentials(token, consumerKey, tokenSecret, consumerSecret)

  val signatureMethod = OAuthParams.HMAC_SHA1
  val scheme = "https"
  val oauthVersion = OAuthParams.ONE_DOT_OH

  val oauthStr = """OAuth oauth_consumer_key="%s",
                   oauth_nonce="%s",
                   oauth_signature="%s",
                   oauth_signature_method="%s",
                   oauth_timestamp="%s",
                   oauth_token="%s",
                   oauth_version="%s"
                 """.replaceAll(",\\s+", ", ")

  def buildHeader() = {
    val timestampSecs = Time.now.inSeconds
    val timestampStr = timestampSecs.toString
    val nonce = util.UUID.randomUUID().toString
    val oauthParams = OAuth1Params(creds.token, creds.consumerKey, nonce, timestampSecs, timestampStr, null, signatureMethod, oauthVersion)
    val normStr = StandardNormalizer(scheme, host, port, "GET", path, List(), oauthParams)
    val sig = StandardSigner
    val signed: String = Signer()(normStr, creds.tokenSecret, creds.consumerSecret)
    oauthStr.format(creds.consumerKey, nonce, URLEncoder.encode(signed, "UTF-8"), signatureMethod, timestampSecs, creds.token, oauthVersion).trim
  }

  val clientFactory: ServiceFactory[HttpRequest, StreamResponse] = ClientBuilder()
    .codec(Stream())
    .tls(host)
    .hosts(hostAndPort)
    .tcpConnectTimeout(1.second)
    .hostConnectionLimit(1)
    .buildFactory()

    
    case class TweetText(text: String)
    DetectorFactory.loadProfile("/home/laem/juno/hello-finagle/profiles");
    

  def go {

    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
    request.setHeader("Authorization", buildHeader())
    request.setHeader("User-Agent", "Finagle 4.0.2 - Liftweb")
    request.setHeader("Host", hostAndPort)

    val client = clientFactory.apply()()
    val streamResponse = client(request)
    streamResponse.onSuccess {
      streamResponse => {
        var messageCount = 0 // Wait for 1000 messages then shut down.
        val startTime= Time.now.inMilliseconds
        streamResponse.messages foreach {
          buffer =>
            messageCount += 1
            
            if (messageCount < 1000){
	            val tweet = buffer.toString(CharsetUtil.UTF_8)
	            try{
	              val tt=parse[TweetText](tweet)
	              
	              val detector = DetectorFactory.create();
	              detector.append(tt.text);

	              val lang = detector.detect();
	              if (lang == "en") {
	                  WikiMinerClient.go(tt.text)
	                  //TODO Wait for a message from Miner, printing the tweet only if concepts were recognized.
	                  // -> actors, offers...
	              }
	                
	              
	              

	            } catch {
	              case e: ParsingException => println("No tweet text found")
	              case e: Exception => println("??")
	            }
	            println("|"+messageCount+"|")
	            
            } else {
              client.release()
              clientFactory.close()
            }
            // We return a Future indicating when we've completed processing the message.
            Future.Done
        }
      }
    }

  }
}