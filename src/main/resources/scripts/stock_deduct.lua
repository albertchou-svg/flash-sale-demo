-- KEYS[1]: 商品庫存的 Key (例如 product:stock:1)
-- ARGV: 不需要參數，預設扣 1

-- 1. 檢查 Key 是否存在
if (redis.call('exists', KEYS[1]) == 1) then
    -- 2. 取得目前庫存
    local stock = tonumber(redis.call('get', KEYS[1]));

    -- 3. 判斷庫存是否 > 0
    if (stock > 0) then
        -- 4. 扣庫存
        redis.call('decr', KEYS[1]);
        return 1; -- 成功
    end
end

return 0; -- 失敗 (沒庫存或商品不存在)