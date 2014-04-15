import pyactivemq

from pyactivemq import ActiveMQConnectionFactory
from pyactivemq import AcknowledgeMode

connectionFactory = ActiveMQConnectionFactory('tcp://ws097.diamond.ac.uk:61616')
connection = connectionFactory.createConnection()
session = connection.createSession(AcknowledgeMode.AUTO_ACKNOWLEDGE)
queue = session.createQueue("testQ");

consumer = session.createConsumer(queue);
connection.start();

print ("Python consumer listening")
while (True):
    m = consumer.receive(1000);
    if (m is None):
        continue;
    
    if (isinstance(m, pyactivemq.TextMessage)):
        print (m.text)
 
    # We cannot deal with Java objects being sent over in the python - well durh!
    # The message from the tesst producer is still reveived but we have to parse it.
    # Instead maybe JSON strings.
    else:
        print(m) # ObjectMessage
