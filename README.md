# 1.Uputstvo za pokretanje

# Da bi projekat mogao da se pokrene lokalno, potrebno je pratiti sledeće korake:

# 1.1. Instalacija ZooKeeper-a

# 1\.	Preuzeti ZooKeeper sa Apache ZooKeeper download stranice.(link)

# 2\.	Raspakovati arhivu i preći u direktorijum:

# cd apache-zookeeper-<verzija>

# 

# 3\.	Podesiti conf/zoo.cfg uputsvo na sajtu (link)

# 4\.	Pokrenuti ZooKeeper server:

# bin/zkServer.sh start   # Linux/Mac 

# bin/zkServer.cmd        # Windows

# 

# 1.2. Kloniranje projekta

# Klonirati repozitorijum sa GitHub-a:

# git clone https://github.com/TatomirUros/zookeeper.git

# 1.3. Build i pokretanje

# Buildovati projekat pomoću Maven-a:

# mvn clean install

# 

# Pokrenuti aplikaciju:

# java -jar target/<ime-jar-fajla>.jar --server.port=8081

# java -jar target/<ime-jar-fajla>.jar --server.port=8082

# java -jar target/<ime-jar-fajla>.jar --server.port=8083

# 1.4. Testiranje

# Aplikacija nakon pokretanja možete pristupiti najlakše putem swagger-a na adresi:

# http://localhost:8082/swagger-ui/index.html#/api-requests/predict

# (uz prilagođen port)



