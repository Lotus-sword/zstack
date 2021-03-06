#!/bin/sh
set -e
#If some arguments are "", the script will be called failed, since shell can't 
#recognize "", when sending it through arguments. 
echo "$0 $*"
user="$1"
password="$2"
host="$3"
port="$4"
zstack_user_password="$5"

base=`dirname $0`

flyway="$base/tools/flyway-3.2.1/flyway"
flyway_sql="$base/tools/flyway-3.2.1/sql/"


mysql --user=$user --password=$password --host=$host --port=$port << EOF
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "$password";
grant all privileges on zstack_rest.* to root@'%' identified by "$password";
grant all privileges on zstack.* to root@'localhost' identified by "$password";
grant all privileges on zstack_rest.* to root@'localhost' identified by "$password";
EOF

rm -rf $flyway_sql
mkdir -p $flyway_sql

cp $base/db/V0.6__schema.sql $flyway_sql
cp $base/db/upgrade/* $flyway_sql

url="jdbc:mysql://$host:$port/zstack"
bash $flyway -user=$user -password=$password -url=$url clean
bash $flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

cp $base/db/V0.6__schema_buildin_httpserver.sql $flyway_sql

url="jdbc:mysql://$host:$port/zstack_rest"
bash $flyway -user=$user -password=$password -url=$url clean
bash $flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

hostname=`hostname`

[ -z $zstack_user_password ] && zstack_user_password=''

db_version=`mysql --version | awk '/Distrib/{print $5}' |awk -F'.' '{print $1}'`
if [ $db_version -ge 10 ];then
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
drop user if exists zstack;
drop user if exists zstack_rest;
create user 'zstack' identified by "$zstack_user_password";
create user 'zstack_rest' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
flush privileges;
EOF
else
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'zstack'@'localhost';
grant usage on *.* to 'zstack'@'%';
drop user zstack;
create user 'zstack' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
flush privileges;
EOF
fi

