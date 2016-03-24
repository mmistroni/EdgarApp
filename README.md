# EdgarApp
A Scala App to download Edgar Data. 
It's my first pet-project in Scala, so it has room for improvements

The application does the following:
- load latest index file
- filter out all lines corresponding to a specific edgar filings
- download the file in memory
- output its XML Content

By default the jar that you build with sbt assembly will kick off src/main/scala/EdgarActorRunner which will try
to download filing informations filtering on 
- cik (("886982", "19617", "1067983"))     // GS, JPM. BRKB, all kind of filings
- form type (13F-HR, which is institutional investors filing)

EdgarActorRunner launch few actors in order to do its processing.
There's also an example code using Futures (src.main.scala.EdgarFutureRunner), and a standalone example in
src.main.scala.examples.ExampleApp

The app also contains unit tests using jUnit, Mockito and testkit (akka framework for testing Actors)
Feel free to mail me at mmistroni@gmail.com for improvement/suggestions.

Thanks

NOTE:

The current implementation is limited to download 5 files at the same time. INcreasing the 
number of concurrent connection above this number will result in dropped connections from
the FTP Server until only 5 files can be downloaded at the same time

