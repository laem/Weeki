package org.laem.TwitterStreaming

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util.CharsetUtil
import com.twitter.util.Future
import java.net.URLEncoder
import com.codahale.jerkson.Json._



object WikiMinerClient {
    // Construct a client, and connect it to the Wikipedia Miner Service from the Waikato university
    val hostAndPort = "wikipedia-miner.cms.waikato.ac.nz:80"
      
    case class ServiceResponse(detectedTopics : List[Map[String, Any]], wikifiedDocument: String)
      
    // Query the service with a human readable text, returns a List of wikipedia concepts
    def go(hrtext : String) = {
      

    val path = "/services/wikify?source="+URLEncoder.encode(hrtext, "UTF-8")+"&responseFormat=json&minProbability=0.4"  

    val client: Service[HttpRequest, HttpResponse] = ClientBuilder()
      .codec(Http())
      .hosts(hostAndPort)
      .tcpConnectTimeout(1.second)
      .hostConnectionLimit(1)
      .build()

    // Issue an HTTP request, respond to the result
    // asynchronously:
    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
    request.setHeader("User-Agent", "Weeki 0.1")
    request.setHeader("Host", hostAndPort)
    
    
   val responseFuture: Future[HttpResponse] = client(request) 
   responseFuture onSuccess {response => {
   		val wikified = parse[ServiceResponse](response.getContent.toString(CharsetUtil.UTF_8).toString())
		val conceptList = wikified.detectedTopics.map(topicMap => topicMap("title"))
		println("\n ---tweet--- "+wikified.wikifiedDocument)
		println(" --concepts- "+conceptList)
   	  	
   	  	
   }
   		
   		
   		
   		
   		} onFailure { error =>
   		error.printStackTrace()
   		} ensure {
   			// All done! Close TCP connection(s):
   			client.release()
   		}
   		
      
    }
    
  
}
