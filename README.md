# EdgarApp
Simple Scala App to download Edgar Data

There's one module (EdgarFutureRunner) which is a sample on how to connect to a repository
using Futures

And then there's one module (EdgarActorRunner) which downloads edgar filing files
using Actors.

The current implementation is limited to download 5 files at the same time. INcreasing the 
number of concurrent connection above this number will result in dropped connections from
the FTP Server until only 5 files can be downloaded at the same time

There are also Unit tests exercising actors (using testkit) and other modules
using Mockito