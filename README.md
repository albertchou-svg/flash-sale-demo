# flash-sale-demo
練習
Java17
(一)Spring Boot Framework:
    Spring AOP /使用Spring Boot 操作Mysql(Transaction) /使用Spring Boot操作Mongodb(原子性使用)
    
(二)資料庫
    Mysql、Mongodb 
    
(三)Zookeeper(curator 套件使用)
    分布式服務框架
    功能/分散式鎖
    
(四)Kafka
    MessageQueue
    Producer/Consumer/Partition
    
(五)Redis(Redission)
    功能/分散式鎖/Lua原子性操作
    
(六)Hazelcast: 分散式計算/DataGrid

(七)OpenAPI : API文件撰寫

(八)單元測試 : Junit5/Mockito

(九)Jmeter (高併發測試)
----------

這是一個搶購商品的系統
User->搶購商品->Redis做第一層數量控管->Kafka發起搶購訊息->Kafka和DB做數量同步(原子性)
