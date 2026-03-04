
//分页查询返回数据类型
export interface PageResult<T> {
  records: T[];
  total: number;
}