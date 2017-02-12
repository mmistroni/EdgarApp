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
The application relies on AWS (lib\awsutilities.jar, not committed in github yet), as i have setup my own services there:
- an SES Service
- S3 buckets
- few SNS Topics

There's a trait, S3Sink, which is in charge of storing data in S3 and Sending an email via SES.

If you don't have an AWS Client, you can still try the application by running examples.ExampleApp, which will download
by default Form '4' (insider trading) and output results to console.

      scala -cp target\scala-2.11\edgarretriever.jar examples.ExampleApp


There's also a Spark application in edgar.spark package which uses a master.gz file containing all edgar filings
for a quarter in 2016


I run it with the following command as i have an AWS Account and AWS Services, but you can override where you want to
dump the data by configuring your own sink in default.EdgarActorsRunner


     scala  
             -Dsmtp.recipients=<csv of email addresses>  
             -Daws.accessKey=<awsAccessKey>  
             -Daws.secretKey=<awsSecretKey> 
             -Daws.bucketName=<awsbucket where to dump the file>
             target\scala-2.11\edgarretriever.jar



The app also contains unit tests using jUnit, Mockito and testkit (akka framework for testing Actors)
Feel free to mail me at mmistroni@gmail.com for improvement/suggestions.

Thanks

NOTE:

There are also Unit tests exercising actors (using testkit) and other modules
using Mockito

