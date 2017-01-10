eng_words

CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 2 };
CREATE TABLE users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login));
*************************************************

CREATE KEYSPACE IF NOT EXISTS geo WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 2 };
CREATE TYPE point(lan double, lon double);
CREATE TABLE ips(ip text, point frozen<point>, city text, country text, hash text, PRIMARY KEY (ip));
CREATE TABLE point_by_ip(point frozen<point>, city text, country text, hash text, PRIMARY KEY (point));
CREATE MATERIALIZED VIEW ip_by_country AS SELECT * from ips  where country IS NOT NULL PRIMARY KEY (country, ip);
CREATE MATERIALIZED VIEW ip_by_city AS SELECT * from ips  where city IS NOT NULL PRIMARY KEY (city, ip);
CREATE MATERIALIZED VIEW ip_by_hash AS SELECT * from ips where hash IS NOT NULL PRIMARY KEY (hash, ip);

select count(*) from ips;
select * from ips limit 100;

select * from ips where ip = '2.248.132.128';
//'5.19.9.94';
select * from point_by_ip where point = {lan: 35.6427, lon: 139.7677};
select * from ip_by_country where country = 'Japan';
select * from ip_by_country where country = 'Sweden';
select * from ip_by_country where country = 'United Kingdom';

select * from ip_by_city where city = 'Tokyo';
select * from ip_by_city where city = 'Stockholm';


select * from ip_by_hash where hash = 'xn76u2x9z2yg';

*******************************************


//http://www.doanduyhai.com/blog/?p=1859


CREATE TABLE chatter.users(login text, password text, lastname text, firstname text, email text, chat_rooms set<text>, PRIMARY KEY(login));

INSERT INTO chatter.users(login, password, lastname, firstname, email, chat_rooms) VALUES('john doe', ...)  IF NOT EXISTS;

CREATE TYPE chatter.user(login text, firstname text, lastname text);

CREATE TABLE chatter.rooms(room_name text, creation_date timestamp, creator frozen<user>, participants set<frozen<user>>, PRIMARY KEY(room_name));


Use LightWeight Transaction to enforce some invariants.

We use LightWeight Transaction to enforce some invariants.
UPDATE chatter.rooms SET participants = participants + {...}  WHERE room_name = 'scalaz' IF EXISTS;


We use LightWeight Transaction to enforce some invariants.
DELETE chatter.rooms WHERE room_name = 'scalaz' IF creator = 'haghard' AND participants = {...};


The IF creator = … condition ensures that only a creator can delete his own room.
The second condition about participants will ensure that when deleting a room, we do not miss any new entering participants.
Indeed, upon room deletion we must update each participant room list to remove the current room.
Any participant that concurrently joins the room when it is being deleted may not be taken into account, unless we rely on LightWeight Transaction.


CREATE TABLE chatter.chat_room_messages(room_name text, message_id timeuuid, content text, author frozen<user>,
PRIMARY KEY(room_name, message_id)) WITH CLUSTERING ORDER BY (message_id DESC);


There is still a caveat with this design, the chat room messages can not scale to billions, unless you set a fixed TTL on each message.
If you have a very popular chat room, having thousands of participants, the message count can grow very fast over time and reach the technical limit of 2 to 63 physical columns for the partition.


SELECT * FROM chatter.chat_room_messages  WHERE room_name = ‘games’ AND message_id < last_message_id LIMIT 50;


CREATE TABLE chatter.chat_room_messages(
  room_name text,
  time_bucket int, //yyyy-MM-dd
  message_id timeuuid,
  content text,
  author frozen<user>,
  PRIMARY KEY((room_name,time_bucket), message_id)) WITH CLUSTERING ORDER BY (message_id DESC);
