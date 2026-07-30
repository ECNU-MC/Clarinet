package org.ecnumc.clarinet.repository;

import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.mapper.AssistanceMapper;
import org.ecnumc.clarinet.po.Assistance;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 求助历史仓库
 *
 * @author liudongyu
 */
public class AssistanceQueryRepository extends BaseRepository<AssistanceMapper> {
	/**
	 * 构造函数
	 * @param sqlSessionFactory SQL 上下文
	 */
	public AssistanceQueryRepository(SqlSessionFactory sqlSessionFactory) {
		super(sqlSessionFactory, AssistanceMapper.class);
	}

	/**
	 * 查询用户最近几条求助内容
	 * @param uid 用户 ID
	 * @param limit 限制最大条数
	 * @return 用户最近几条求助内容
	 */
	public List<Assistance> queryByUid(String uid, int limit) {
		return this.execute(mapper -> mapper.queryByUid(uid, limit));
	}

	/**
	 * 查询符合条件的用户求助总数
	 * @param uid 用户 ID，可空
	 * @param date 日期
	 * @param dealt 是否被解决，留空则不作为筛选条件
	 * @return 符合条件的用户求助总数
	 */
	public Long query(@Nullable String uid, String date, @Nullable Integer dealt) {
		LocalDate localDate = LocalDate.parse(date);
		ZoneId zone = ZoneId.systemDefault();
		Date begin = Date.from(localDate.atStartOfDay().atZone(zone).toInstant());
		Date end = Date.from(localDate.plusDays(1).atStartOfDay().atZone(zone).toInstant());
		if(uid == null) {
			if(dealt == null) {
				return this.execute(mapper -> mapper.countAllBetween(begin, end));
			}
			return this.execute(mapper -> mapper.countAllDealtBetween(begin, end, dealt));
		}
		if(dealt == null) {
			return this.execute(mapper -> mapper.countByUidBetween(uid, begin, end));
		}
		return this.execute(mapper -> mapper.countByUidDealtBetween(uid, begin, end, dealt));
	}

	/**
	 * 根据关键词搜索求助记录
	 * @param keywords 正向关键词
	 * @param excludedKeywords 负向关键词
	 * @param limit 限制最大条数
	 * @return 符合条件的求助记录
	 */
	public List<Assistance> queryByKeywords(List<String> keywords, List<String> excludedKeywords, int limit) {
		if(!excludedKeywords.isEmpty()) {
			String query = "+" + String.join(" +", keywords) + " -" + String.join(" -", excludedKeywords);
			return this.execute(mapper -> mapper.queryByKeywordsBooleanMode(query, limit));
		}
		return this.execute(mapper -> mapper.queryByKeywords(String.join(" ", keywords), limit));
	}
}
