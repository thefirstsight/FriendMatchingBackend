# 伙伴匹配系统

## 需求分析

* 用户添加标签，标签的分类
  * 如学习方向 java/cpp/python
  * 工作/大学
* 主动搜索
  * 根据标签进行搜索
  * 借助redis缓存
* 组队
  * 创建队伍
  * 加入队伍
  * 根据标签查询队伍
  * 邀请其他人
* 允许用户去修改标签
* 推荐
  * 相似度计算算法
  * 本地分布式计算 
