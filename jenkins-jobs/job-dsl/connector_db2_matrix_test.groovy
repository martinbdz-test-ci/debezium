// Job definition to test PostgreSQL connector against different PostgreSQL versions

matrixJob('connector-debezium-db2-matrix-test') {

    displayName('Debezium DB2 Connector Test Matrix')
    description('Executes tests for DB2 Connector')
    label('Slave')

    axes {
        label("Node", "Slave")
    }

    properties {
        githubProjectUrl('https://github.com/debezium/debezium-connector-db2')
    }

    parameters {
        stringParam('REPOSITORY', 'https://github.com/debezium/debezium-connector-db2', 'Repository from which connector is built')
        stringParam('BRANCH', 'main', 'A branch/tag from which the connector is built')
        stringParam('SOURCE_URL', "", "URL to productised sources")
        booleanParam('PRODUCT_BUILD', false, 'Is this a productised build?')
    }

    triggers {
        cron('H 04 * * 1-5')
    }

    wrappers {
        preBuildCleanup()

        timeout {
            noActivity(1200)
        }
    }

    publishers {
        archiveJunit('**/target/surefire-reports/*.xml')
        archiveJunit('**/target/failsafe-reports/*.xml')
        mailer('jpechane@redhat.com', false, true)
    }

    logRotator {
        daysToKeep(7)
    }

    steps {
        shell('''
# Ensure WS cleaup
ls -A1 | xargs rm -rf

# Retrieve sources
if [ "$PRODUCT_BUILD" == true ] ; then
    curl -OJs $SOURCE_URL && unzip debezium-*-src.zip
    pushd debezium-*-src
    pushd $(ls | grep -P 'debezium-[^-]+.Final')

    # Build parent
    mvn clean install -s ~/.m2/settings-snapshots.xml -am -fae \
        -DskipTests -DskipITs \
        -Dinsecure.repositories=WARN \
        -Ppnc

    # Run connector tests
    popd
    pushd debezium-connector-db2-*
    mvn clean install -U -s $HOME/.m2/settings-snapshots.xml -am -fae \
        -Dmaven.test.failure.ignore=true \
        -Dtest.argline="-Ddebezium.test.records.waittime=5" \
        -Dinsecure.repositories=WARN \
        -Ppnc \
        $MAVEN_ARGS
else
    git clone $REPOSITORY .
    git checkout $BRANCH

    # Run connector tests
    mvn clean install -U -s $HOME/.m2/settings-snapshots.xml -am -fae \
        -Dmaven.test.failure.ignore=true \
        -Dtest.argline="-Ddebezium.test.records.waittime=5" \
        -Dinsecure.repositories=WARN \
        $MAVEN_ARGS
fi
''')
    }
}
