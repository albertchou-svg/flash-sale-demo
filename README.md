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
Hazelcast做黑名單列表、系統廣播
Zookeeper做分散式鎖控管 (另一種購買方式)
Mongodb寫log

-------------------------------
指令紀錄

# 使用 Winget
winget install stern
# 它會監聽所有名稱包含 "flash-sale" 的 Pod
stern flash-sale

# --prefix 會顯示 Log 是來自哪個 Pod
# -l app=flash-sale 會選取所有符合標籤的 Pod
kubectl logs -f -l app=flash-sale --prefix

# 明確指定要啟動的服務，跳過 flash-sale-app
docker-compose up -d mysql redis zookeeper kafka mongodb
# 更改設定時應用設定檔  hpa自動擴充設定
kubectl apply -f k8s-hpa.yaml
# app的設定
kubectl apply -f k8s-deployment.yaml
# 打包JAR
./mvnw clean package -DskipTests
# docker build 
docker build -t flash-sale-app:k8s-v1 .
# 強制重啟pod
kubectl rollout restart deployment/flash-sale-app
# 查看pod狀態
kubectl get pods -w

#啟動多節點叢集
kind create cluster --config kind-multi-node.yaml --name my-cluster

#查看節點狀態
kubectl get nodes

# 把 Image 載入到 Kind 的節點裡
kind load docker-image flash-sale-app:k8s-v1 --name my-cluster

#確認分布
kubectl get pods -o wide

#當本機上有多個k8s時可以確認狀態
kubectl config get-contexts

#切換不同的k8s
kubectl config use-context kind-my-cluster