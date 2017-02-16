create keyspace examples if not exists
 with replication = {
 'class' : 'SimpleStrategy',
 'replication_factor' : 1
 };
