# TDR prototype: GraphQL API

This project is part of the [Transfer Digital Records][tdr-docs] project. It is a prototype of the API that will be used
to get and store information about transferred files and the progress of the transfer.

It uses the Scala library [Sangria] to respond to GraphQL queries.

[tdr-docs]: https://github.com/nationalarchives/tdr-dev-documentation
[Sangria]: https://sangria-graphql.org/

## Design

The application is composed of two sbt projects: the core GraphQL logic and the entry point, which is an akka-http
server.

### Database migrations

This project depends on a Postgres database. It uses [Flyway] to run database migrations to keep the database in sync
with the code.

Migrations are run quite differently in the development and deployed environments, so see the sections below for more
information on how to run them. 

[Flyway]: https://flywaydb.org/

## Development

### Prerequisites

Set up a PostgreSQL database. One option is to run a [Postgres Docker image][postgres-docker].

#### Docker Setup

When setting up the docker container ensure the run command is as follows, including the port information before the container name argument:

```
$ docker run --name some-postgres -e POSTGRES_PASSWORD=yourpassword -d -p 5432:5432 postgres
```

Follow the Docker guide to run the image and set the database password. Connect to your image with psql and create a
database:

```
psql -h localhost -p 5432 -U postgres -W
CREATE DATABASE tdrapi
```

Then run the database migrations (see below) to create the tables and seed data.

[postgres-docker]: https://hub.docker.com/_/postgres

### Database migrations

When you first set up the project, or if new migrations have been added to the project, you will need to run the Flyway
DB migrations.

[Download and install the Flyway CLI][flyway-install].

Create a Flyway config file by copying migrations/conf/flyway.conf.template to migrations/conf/flyway.conf, and updating
the values to match your local Postgres database.

From the project root directory, run:

```
flyway -X -configFiles=migrations/conf/flyway.conf -locations=filesystem:migrations/sql/ migrate
```

[flyway-install]: https://flywaydb.org/download/

### Run the server

To run the project in development from the command line, run:

```
DB_URL=jdbc:postgresql://localhost/tdrapi DB_USERNAME=postgres DB_PASSWORD=abcde sbt run
``` 

updating the environment variables with your local DB settings.

To run in IntelliJ, either run the `ApiServer` app or create an sbt configuration to run `sbt run`. Set the same
environment variables as in the CLI option above. 

Both options will start a server at http://localhost:8080/. It currently just provides a GraphQL POST endpoint at
http://127.0.0.1:8080/graphql, which you can send queries to with curl, Postman or a GraphQL client.

### Generate GraphQL schema

To generate a schema from the Sangria code to use in Postman or a client application, run:

```
sbt graphqlSchemaGen
```

This will output a schema file to `target/sbt-graphql/schema.graphql`.

To use the schema in Postman, see the [Postman guide to importing GraphQL schemas][postman-import-graphql].

[postman-import-graphql]: https://learning.getpostman.com/docs/postman/sending_api_requests/graphql/#importing-graphql-schemas

## Deployment

### Infrastructure

The API is deployed to AWS ECS. The infrastructure is defined in the [TDR Terraform config][tdr-terraform].

[tdr-terraform]: https://github.com/nationalarchives/tdr-prototype-terraform

### Deploy the API

- Build the lambda sbt project locally by running `sbt clean assembly`, which should build a jar file at
  target/scala-2.12/tdr-api.jar
- Build the Docker image by running `docker build . --tag nationalarchives/sangria:env-name`
- Push the image to Docker Hub with `docker push nationalarchives/sangria:env-name`

Then start a container running the new image. You can do this in the AWS Console or on the command line.

In the AWS Console:

- Navigate to the cluster in ECS
- Check the `sangria-graphql-service-env-name` service and click Update
- Select "Force new deployment" and click "Skip to review"

Alternatively, run the AWS CLI:

```
aws ecs update-service --service sangria-graphql-service-dev --cluster tdr-graphql-ecs-dev --force-new-deployment
```

ECS will start a new service running the new image, switch the load balancer over to the new service, and shut the old
service down. The whole process takes about 10 minutes.

### Database migrations

We don't have currently have a stable CI environment, and we cannot connect to the DB from our dev machines because of
network security rules. This means that the easiest place to run database migrations is in an AWS environment like an
ECS container.

To deploy the migrations, `cd` to the migrations folder and run:

```
docker build . --tag nationalarchives/tdr-prototype-db-migrations:env-name
docker push nationalarchives/tdr-prototype-db-migrations:env-name
```

where `env-name` is a Terraform environment name, e.g. `dev` or `test`.

In the AWS console, go to the ECS cluster and run the task you defined earlier. Configure these settings:

- Set the launch type to "Fargate"
- Set the VPC to be the same as the one that the database is in - hover over a VPC ID to see its name, e.g.
  `ecs-vpc-dev`
- Choose any subnet in the VPC
- **Set the security group**, and choose the existing security group `migration-task-security-group-<env-name>` that
  matches the environment you want to migrate

### Test the deployed API

You should then be able to POST GraphQL queries to your API Gateway URL (remembering to add `/graphql` to the end).

## Postman Example Queries and Mutations

Some examples of queries and mutations for use with Postman.

### "```GetConsignments```"

```
{ 
       getConsignments {
           name,
           id,
           series {
               id,
               name,
               description
           }
       }
   }
```
### "```CreateConsignments```"
```
mutation { 
    createConsignment(name: "tkConsignment1", seriesId: 1) {
        name,
        id,
        series {
            id,
            name,
            description
        }
    }
}
```
### "```GetFile```"
```
{ 
    getFile(id: "some-file-id-uuid") {
        path,
        id,
        consignmentId,
        fileSize,
        fileName,
        lastModifiedDate
     }
}
```
### "```GetFiles```"
```{ 
       getFiles {
           path,
           id,
           consignmentId,
           fileSize,
           fileName,
           lastModifiedDate
       }
   }

```
### "```CreateFile```"

#### Postman Query
```
mutation($input: CreateFileInput!) { 
    createFile(createFileInput: $input) {
        path,
        id,
        consignmentId,
        fileSize,
        fileName,
        lastModifiedDate
    }
}
```

#### Postman GraphQl Variables
```
{
    "input": {	     
        "fileSize": 11,
        "fileName": "file1",
        "lastModifiedDate": "12345667",
        "path": "file/path/file1.txt", 
        "consignmentId": 1,
        "clientSideChecksum": "abcd12345"
    }
}
```

### "```CreateMultipleFiles```"

#### Postman Query
```
mutation($input: [CreateFileInput!]!) { 
    createMultipleFiles(createFileInputs: $input) {
        path,
        id,
        consignmentId,
        fileSize,
        fileName,
        lastModifiedDate
    }
}
```

#### Postman GraphQl Variables
```
{
    "input": [
        {
            "fileSize": 11,
            "fileName": "file1",
        	"lastModifiedDate": "12345667",
        	"path": "file/path/file1.txt", 
        	"consignmentId": 1,
        	"clientSideChecksum": "abcd12345"
        },
        {
        	"fileSize": 11,
        	"fileName": "file2",
        	"lastModifiedDate": "12345667",
        	"path": "file/path/file2.txt", 
        	"consignmentId": 1,
        	"clientSideChecksum": "abcd12345"
        },
        {
        	"fileSize": 11,
        	"fileName": "file3",
        	"lastModifiedDate": "12345667",
        	"path": "file/path/file3.txt", 
        	"consignmentId": 1,
        	"clientSideChecksum": "abcd12345"
        }
    ]
}
```
