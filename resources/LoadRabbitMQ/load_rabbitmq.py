# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
import pika
import sys

if len(sys.argv) > 1:
    filename = sys.argv[1]
else:
    filename = 'addresses'

f = open(filename,'r')

credentials = credentials = pika.PlainCredentials('gigapower', 'gigapower')

connection = pika.BlockingConnection(pika.ConnectionParameters(host = 'localhost', credentials=credentials))

channel = connection.channel()

channel.queue_delete(queue='gigapower')

channel.queue_declare(queue='gigapower', durable=True)

channel.queue_purge(queue='gigapower')

for item in f.readlines():
    channel.basic_publish(exchange='',
                        routing_key='gigapower',
                        body=item.strip(),)


connection.close()
