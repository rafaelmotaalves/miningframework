# Mining Framework
[![Build Status](https://travis-ci.org/rafaelmotaalves/miningframework.svg?branch=master)](https://travis-ci.org/rafaelmotaalves/miningframework)


This is a framework for mining and analyzing git projects.

We focus on analyzing merge commits, although this could be easily changed to analyze any kind of commit.

We basically have variability points (hot spots) for 
* preprocessing the set of projects to be analyzed (like forking projects and enabling Travis CI services for such forks)
* filtering the set of merge commits in such projects (like for focusing only on merge commits with parents that involve changes to the same method)
* collecting experimental data from each merge commit (like revisions of the files declaring the method that was changed in both parents, commit hashes, line numbers of the changes in each parent, overall statistics about the merge commit, result of replaying the merge operation with different tools, etc.)
* postprocessing the collected experimental data (like aggregating and summarizing data, or any kind of operation that is more expensive to perform in a per merge commit basis, such as downloading Travis CI ".jar" files for each merge revision, merging spreadsheets created by different data collectors, etc.), after all projects have been analyzed

We also have a number of implementations for such variability points, so that one can reuse or adapt them as needed for instantiating the framework.
The examples illustrated above correspond to some of the implementations we provide here.


## Getting Started
* Fork and clone the project. If you want to run the project tests, you must clone the repository with the recursive option:
 ``` git clone --recursive https://github.com/spgroup/miningframework ```

* This project uses [Apache Groovy](http://groovy-lang.org/). You have to install version 2.5.x or newer to use the framework and start mining projects.

* For one of the implementation of the postprocessing variability point ([OutputProcessorImpl](https://github.com/spgroup/miningframework/tree/master/src/services/OutputProcessorImpl.groovy)), you also have to install [Python](https://www.python.org/) version 3.7.x or newer. This is needed for a script that fetches build files from Travis CI, and another script that converts collected data to a format that is used by the SOOT static analyses invoked by this instantiation. If you don't wish to use this specific implementation of the postprocessing variability point, there is no need to install Python.


## Instantiating or extending the framework

You need to implement the following interfaces (see [interfaces/](https://github.com/spgroup/miningframework/tree/master/src/main/interfaces)) or choose their existing implementations (see [services/](https://github.com/spgroup/miningframework/tree/master/src/services/)):
* ProjectProcessor 
* CommitFilter
* DataCollector 
* OutputProcessor 

They correspond to the four variability points described at the beginning of the page.

The framework uses [Google Guice](https://github.com/google/guice) to implement dependency injection, and inject the interface implementations. 
So, to select the interface implementations you want to use in your desired instantiation of the framework, you also need to write a class such as [MiningModule](https://github.com/spgroup/miningframework/blob/master/src/services/MiningModule.groovy), which acts as the dependency injector. This one, in particular, is used as a default injector if no other is specified when invoking the framework.


## Running a specific framework instantiation

You can run the framework by including the [src](https://github.com/spgroup/miningframework/blob/master/src) directory in the classpath and executing `src/main/app/MiningFramework.groovy`.

This can be done by configuring an IDE or executing the following command in a terminal:
* Windows/Linux/Mac: `groovy -cp src src/main/app/MiningFramework.groovy [options] [input] [output]`

`[input]` is the path to a CSV file containing the list of projects to be analyzed (like [projects.csv](https://github.com/spgroup/miningframework/blob/master/projects.csv)), one project per line. The list can contain external projects to be downloaded by the framework (the path field should be an URL to a git project hosted in the cloud), or local projects (the path field should refer to a local directory).

`[output]` is the path to a directory that the framework should create containing the results (collected experimental data, statistics, etc.) of the mining process.  

`[options]` a combination of our command line configuration options. It's useful to type `--help` in the `[options]` field to see the supported options and associated information.

> The options are available to all variability points implementations, but some of the implementations might not make use of all options. So check the documentation of the variability points implementations you need to confirm that they really make use of the options of interest. 

> If you intend to use the framework multithreading option, be aware of the need to synchronize the access to output files or state manipulated by the implementations of the framework variability points.

> For example, for running the study we use as an example to illustrate the variability points at the beginning of the page, we invoke the following command at the project top folder: 
    * Windows/Linux/Mac: `groovy -cp src src/main/app/MiningFramework.groovy --access-key github-personal-access-token --threads 2 ./projects.csv SOOTAnalysisOutput`

> For the used variability point implementation, the provided GitHub [personal access token](https://github.com/settings/tokens) (opt for repo scope) should be associated with a GitHub account also registered in [Travis](https://travis-ci.org/). Forks will be created for each project, the builds will be generated via Travis, and deployed to the forks as GitHub releases.

> The CLI has the following help page:
```
usage: miningframework [options] [input] [output]
the Mining Framework take an input csv file and a name for the output dir
(default: output)
 Options:
 -a,--access-key <access key>   Specify the access key of the git account
                                for when the analysis needs user access to
                                GitHub
 -h,--help                      Show help for executing commands
 -i,--injector <class>          Specify the class of the dependency
                                injector (Must provide full name, default
                                src.services.MiningModule)
 -k,--keep-projects             Keep projects in disk after analysis
 -p,--push <link>               Specify a git repository to upload the
                                output in the end of the analysis (format
                                https://github.com/<owner>/<name>
 -s,--since <date>              Use commits more recent than a specific
                                date (format DD/MM/YYY)
 -t,--threads <threads>         Number of cores used in analysis (default:
                                1)
 -u,--until <date>              Use commits older than a specific
                                date(format DD/MM/YYYY)
```


## Testing
One can run the framework tests by including `src` in the classpath and executing `src/test/AllTestsSuite.groovy`

This command runs both unit and integration tests, to run them separately you can run:
`groovy -cp "src" src/test/unit/TestSuite.groovy`
or
`
groovy -cp "src" src/integration/TestSuite.groovy
`

For example, for running the study we use as an example to illustrate the variability points at the beginning of the page, we invoke the following command at the project top folder: 
* Windows/Linux/Mac: `groovy -cp src src/main/app/MiningFramework.groovy --access-key github-personal-access-token --threads 2 ./projects.csv SOOTAnalysisOutput`

* To create new tests, you have to create a git repository with a merge scenario simulating a specific situation you want to test, add it to the `test_repositories` directory, add a corresponding entry to `src/test/input.csv`, and then create the Test class.
