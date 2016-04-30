# EdgarApp
A Scala App to download Edgar Data. 
It's my first pet-project in Scala, so it has room for improvements

The application does the following:
- load latest index file
- filter out all lines corresponding to a specific edgar filings
- download the file in memory
- output its XML Content

By default the jar that you build with sbt assembly will kick off src/main/scala/EdgarActorRunner which will try
to download filing informations filtering on form type (13F-HR, which is institutional investors filing).
At the end of the processing the code will send an email containing shares held by major institutional investors.
In order to do that you will need to provide the following system properties:
 to send an email with e :
-Dsmtp.host  = smtp host name you want to use to send email
-Dsmtp.port  = smtp port 
-Dsmtp.username = username for connecting to smtp host
-Dsmtp.password = password for the smtp host
-Dsmtp.recipients = recipients of the email (comma separated address)

Example command line:

scala -Dsmtp.host=<yoursmpthost> 
      -Dsmtp.port=<yoursmtpport>
      -Dsmtp.username=<smtp username> 
      -Dsmtp.password=<smtppassword> 
      -Dsmtp.recipients=<comma separated email addresses>
      target\scala-2.11\edgarretriever.jar




Alternatively, you can run an Example application which will download (and display) a bunch of Form4 filings and display it to output.
To run the exampleApp simply type

       scala -cp target\scala-2.11\edgarretriever.jar examples.ExampleApp




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

There are also Unit tests exercising actors (using testkit) and other modules
using Mockito

TODO:
Implement logic based on ParserCombinators to parse command line parameters
in order to parameterize factories used in application
