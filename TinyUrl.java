短链接服务：
Scenario：就是利用2个记录"已经转化好的""短链接->长链接","长连接->短连接"的映射关系的HashMap 实现 shortToLen() 和 lenToShort() 2个方法
        但是：HashMap只是用来记录已经转化好的结果，不能直接使用 现有的Hash算法来 计算 "长连接" 对应的 "短连接"

Neccassary：设日活跃用户为 100万，其中 100% 的用户都会调用 shortToLen()，只是从HashMap中Query得到"真实url"
    大约只有 1% 的用户调用 lenToShort()，主要负责向"映射关系记录(可以)" insert新的"短->长"的映射关系
    计算QPS：
    Insert：
    per day: 100W * 1%(函数使用频率) * 10 (巅峰请求次数) = 10W
    per second: 10W/ 86400 = 1.2
    LookUp: 100W * 100%(函数使用率) * 3 (巅峰请求次数) = 300W
    per second: 300W/86400 = 35，由于 QPS 

Algorithm：所有使用现有Hash算法来完成 generateShortUrl 的一律得 0 分，因为这把问题复杂化了！！！
我们要实现的效果就是"用尽量少的信息"为"每个longUrl"记录"1个Unique Value"，所以用1个"自增的Index"作为 shortUrl 即可
e.g. longUrl为 www.baidu.com；shortUrl 为 1

public class Shortener {
    map<String, String> longToShort;
    map<String, String> shortToLong;
    
    String insert(String longUrl) { //longToShort
        if (!longToShort.containsKey(longUrl)) {
            String shortUrl = generateShortUrl(longUrl);

            longToShort.put(longUrl, shortUrl);
            shortToLong.put(shortUrl, longUrl);
        }
        return longToShort.get(longUrl);
    }
    //最初始解法，直接将长连接的序号 n 作为 短 url即可
    //但是这种短连接如果只用"整数编码"表示，需要的长度过高，而记录保存在内存中，以致对内存的消耗过大
    int generateShortUrl(String longUrl) {
        return longToShort.size().toString();
    }

    String shortToLong(String shortUrl) {
        return shortToLong.get(shortToLong);
    }
}

KiloByte：每条长短连接的映射记录，{longUrl，约100B} + {shortUrl：约4B} + {state，约4B，用于标识url是否作废}
为了缩小短Url所在Map对内存的占用，将 "encode(长url的序号)" 作为最终的 short url
                          原始整数编码              改进编码方式后
yearly URL数：                365W                    365W
每位编码可用字符数：         [0-9] = 10             [0-9a-zA-Z]
编码长度：              log10(365W) = 7.6 = 8       log62(365W) = 4.2 = 5
短urlExample：            域名 / 36500000            域名/2t9jG

public class Shortener {
    map<String, String> longToShort;
    map<String, String> shortToLong;
    
    String insert(String longUrl) {
        if (!longToShort.containsKey(longUrl)) {
            String shortUrl = generateShortUrl(longUrl);

            longToShort.put(longUrl, shortUrl);
            shortToLong.put(shortUrl, longUrl);
        }
        return longToShort.get(longUrl);
    }
    
    String generateShortUrl(String longUrl) {
        return convertTo62(longToShort.size());
    }

    String convertTo62(int number) {
        char[] encode = {'0'...'9','a'...'z','A'...'Z'};
        String ret = "";
        while (number > 0) {
            int curEncode = number % 62;
            ret = encode[curEncode] + ret;
            number /= 62;
        }
        return ret;
    }

    String shortToLong(String shortUrl) {
        return shortToLong.get(shortToLong);
    }
}


Evolve：
• How  to  support  random (for security or sth)?
    对 Random(0,range)， 而不是 (longUrl 的编号) Encode，生成 shortUrl
• How  to  avoid  conflicting?
    Try  again
• How  to  implement  time-­‐limited  service?
    Expire/state
• How  to  cache?
    Pre-­‐load
    Replacement