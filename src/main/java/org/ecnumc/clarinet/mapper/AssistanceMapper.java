package org.ecnumc.clarinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.ecnumc.clarinet.po.Assistance;

import java.util.Date;
import java.util.List;

/**
 * 解答记录表 Mapper
 *
 * @author liudongyu
 */
public interface AssistanceMapper extends BaseMapper<Assistance> {
	/**
	 * 查询所有解答记录数
	 * @return 所有解答记录数
	 */
	Long countAll();

	/**
	 * 查询用户所有解答记录数
	 * @param uid 用户 ID
	 * @return 用户所有解答记录数
	 */
	Long countByUid(@Param("uid") String uid);

	/**
	 * 查询时间范围内所有解答记录数
	 * @param beginTime 开始时间
	 * @param endTime 结束时间
	 * @return 时间范围内所有解答记录数
	 */
	Long countAllBetween(@Param("begin_time") Date beginTime, @Param("end_time") Date endTime);

	/**
	 * 查询用户时间范围内所有解答记录数
	 * @param uid 用户 ID
	 * @param beginTime 开始时间
	 * @param endTime 结束时间
	 * @return 用户时间范围内所有解答记录数
	 */
	Long countByUidBetween(@Param("uid") String uid, @Param("begin_time") Date beginTime, @Param("end_time") Date endTime);

	/**
	 * 查询时间范围内所有解决/未解决解答记录数
	 * @param beginTime 开始时间
	 * @param endTime 结束时间
	 * @param dealt 是否已解决
	 * @return 时间范围内所有解决/未解决解答记录数
	 */
	Long countAllDealtBetween(@Param("begin_time") Date beginTime, @Param("end_time") Date endTime, @Param("dealt") int dealt);

	/**
	 * 查询用户时间范围内所有解决/未解决解答记录数
	 * @param uid 用户 ID
	 * @param beginTime 开始时间
	 * @param endTime 结束时间
	 * @param dealt 是否已解决
	 * @return 用户时间范围内所有解决/未解决解答记录数
	 */
	Long countByUidDealtBetween(@Param("uid") String uid, @Param("begin_time") Date beginTime, @Param("end_time") Date endTime, @Param("dealt") int dealt);

	/**
	 * 根据 uid 查询用户最近几条提问历史，按 update_at 倒序排列
	 * @param uid 用户 ID
	 * @param limit 条数限制
	 * @return 最近几条提问历史
	 */
	List<Assistance> queryByUid(@Param("uid") String uid, @Param("limit") int limit);

	/**
	 * 根据正向关键词搜索求助记录
	 * @param keywords 关键词
	 * @param limit 限制最大条数
	 * @return 符合条件的求助记录
	 */
	List<Assistance> queryByKeywords(@Param("keywords") String keywords, @Param("limit") int limit);

	/**
	 * 根据正负向关键词搜索求助记录
	 * @param keywords 正负向关键词
	 * @param limit 限制最大条数
	 * @return 符合条件的求助记录
	 */
	List<Assistance> queryByKeywordsBooleanMode(@Param("keywords") String keywords, @Param("limit") int limit);
}
