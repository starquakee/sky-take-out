package com.sky.util;

import com.github.pagehelper.PageHelper;
import com.sky.mapper.DishMapper;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BloomFilter { // 您可以自己命名，比如 BloomFilterService

    private RedissonClient redissonClient;
    private RBloomFilter<Long> bloomFilter;
    private DishMapper dishMapper;

    // 在构造函数中，我们手动创建和配置我们自己的 RedissonClient
    @Autowired
    public BloomFilter(DishMapper dishMapper) {
        this.dishMapper = dishMapper;

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setDatabase(0);

        this.redissonClient = Redisson.create(config);
        System.out.println("独立的 RedissonClient for Bloom Filter 创建成功！");
    }

    // 在服务初始化时，获取布隆过滤器实例
    @PostConstruct
    public void init() {
        // 为布隆过滤器起一个唯一的名字
        this.bloomFilter = redissonClient.getBloomFilter("sky-take-out:dish:category-id:bloom");
        // 预估1000个分类，1%的误判率
        this.bloomFilter.tryInit(1000L, 0.01);

        // 【第3步】在初始化时，增加带分布式锁的数据加载逻辑
        RLock lock = redissonClient.getLock("lock:dish-category-bloom:init");
        try {
            // 尝试获取锁，防止多个实例同时加载数据
            if (lock.tryLock(10, 60, TimeUnit.SECONDS)) {
                System.out.println("获取到布隆过滤器初始化锁，准备加载数据...");
                // 调用数据加载方法
                loadDishCategories();
            } else {
                System.out.println("未能获取到初始化锁，可能其他实例正在加载。");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("布隆过滤器初始化锁获取失败。");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock(); // 确保释放锁
            }
        }
        System.out.println("布隆过滤器初始化流程结束。");
    }

    /**
     * 【第3步】新增的核心方法：从数据库加载菜品分类ID到布隆过滤器
     */
    private void loadDishCategories() {
        // 关键判断：仅当过滤器为空时才执行加载
        if (bloomFilter.count() == 0) {
            System.out.println("菜品分类布隆过滤器为空，开始从数据库加载数据...");

            int pageNum = 1; // PageHelper 页码从1开始
            final int PAGE_SIZE = 500; // 每次查询500条

            while (true) {
                // 1. 设置分页参数
                PageHelper.startPage(pageNum, PAGE_SIZE);

                // 2. 执行查询，PageHelper 会自动应用分页
                List<Long> categoryIds = dishMapper.listAllCategoryIds();

                // 如果当前页查不到任何数据，说明所有数据已加载完毕，跳出循环
                if (categoryIds == null || categoryIds.isEmpty()) {
                    break;
                }

                // 3. 将本页数据批量添加到布隆过滤器
                for (Long categoryId : categoryIds) {
                    this.bloomFilter.add(categoryId);
                }
                System.out.printf("已加载 %d 个分类ID (第 %d 页) 到布隆过滤器.%n", categoryIds.size(), pageNum);
                // 如果查询到的数据量小于每页大小，说明这是最后一页，加载完后跳出循环
                if (categoryIds.size() < PAGE_SIZE) {
                    break;
                }

                pageNum++; // 准备查询下一页
            }

            System.out.println("所有菜品分类ID已加载完毕。");
        } else {
            System.out.println("菜品分类布隆过滤器中已有数据，跳过加载。");
        }
    }

    @PreDestroy
    public void onDestroy() {
        if (this.redissonClient != null) {
            this.redissonClient.shutdown();
        }
    }


    /**
     * 添加一个分类ID
     * @param categoryId
     */
    public void add(Long categoryId) {
        bloomFilter.add(categoryId);
    }

    /**
     * 检查一个分类ID是否可能存在
     * @param categoryId
     * @return
     */
    public boolean mightContain(Long categoryId) {
        return bloomFilter.contains(categoryId);
    }
}
