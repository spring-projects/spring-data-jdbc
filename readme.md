
# Spring Data JDBC 

The primary goal of the [Spring Data](http://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use data access technologies. This module deals with enhanced support for JDBC based data access layers.

## Features ##

## Getting Help ##

## Quick Start ##

## Execute Tests ##

### Fast running tests

Fast running tests can executed with a simple 

    mvn test

This will execute unit tests and integration tests using an in-memory database.

### Running tests with a real database

To run the integration tests against a specific database you nned to have the database running on your local machine and then execute.

    mvn test -Dspring.profiles.active=<databasetype>

This will also execute the unit tests.

Currently the following *databasetypes* are available:

* hsql (default, does not need to be running)
* mysql

### Run tests with all databases

    mvn test -Pall-dbs

This will execute the unit tests, and all the integration tests with all the databases we currently support for testing. The databases must be running.

## Contributing to Spring Data JDBC ##

Here are some ways for you to get involved in the community:

* Get involved with the Spring community by helping out on [stackoverflow](http://stackoverflow.com/questions/tagged/spring-data-jdbc) by responding to questions and joining the debate.
* Create [JIRA](https://jira.spring.io/browse/DATAJDBC) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://spring.io/blog) to spring.io.

Before we accept a non-trivial patch or pull request we will need you to [sign the Contributor License Agreement](https://cla.pivotal.io/sign/spring). Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. If you forget to do so, you'll be reminded when you submit a pull request. Active contributors might be asked to join the core team, and given the ability to merge pull requests.