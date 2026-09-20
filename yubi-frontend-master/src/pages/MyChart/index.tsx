import { deleteChartUsingPOST, listMyChartByPageUsingPOST } from '@/services/yubi/chartController';
import { useModel } from '@@/exports';
import { Avatar, Button, Card, List, message, Modal, Popconfirm, Result, Select, Space, Tag, Typography } from 'antd';
import ReactECharts from 'echarts-for-react';
import React, { useEffect, useState } from 'react';
import Search from 'antd/es/input/Search';

const MyChartPage: React.FC = () => {
  const init = { current: 1, pageSize: 4, sortField: 'createTime', sortOrder: 'desc' };
  const [params, setParams] = useState<API.ChartQueryRequest>(init); const [items, setItems] = useState<API.Chart[]>([]);
  const [total, setTotal] = useState(0); const [loading, setLoading] = useState(true); const [status, setStatus] = useState('all'); const [selected, setSelected] = useState<API.Chart>();
  const { initialState } = useModel('@@initialState');
  const load = async () => { setLoading(true); try { const res = await listMyChartByPageUsingPOST(params); setItems((res.data?.records ?? []).filter((x) => status === 'all' || x.status === status)); setTotal(res.data?.total ?? 0); } catch (e: any) { message.error('获取图表失败，' + e.message); } setLoading(false); };
  useEffect(() => { load(); }, [params, status]);
  return <div className="my-chart-page"><Space><Search style={{ width: 360 }} placeholder="请输入图表名称" enterButton loading={loading} onSearch={(v) => setParams({ ...init, name: v })} /><Select value={status} style={{ width: 140 }} onChange={setStatus} options={[['all', '全部状态'], ['succeed', '已完成'], ['running', '处理中'], ['wait', '等待中'], ['failed', '失败']].map(([value, label]) => ({ value, label }))} /></Space><div className="margin-16" />
    <List grid={{ gutter: 16, xs: 1, sm: 1, md: 1, lg: 2, xl: 2, xxl: 2 }} pagination={{ onChange: (current, pageSize) => setParams({ ...params, current, pageSize }), current: params.current, pageSize: params.pageSize, total }} loading={loading} dataSource={items} renderItem={(item) => <List.Item><Card style={{ width: '100%' }} actions={[<Button type="link" onClick={() => setSelected(item)}>查看详情</Button>, <Popconfirm title="确定删除这张图表吗？" onConfirm={async () => { if (!item.id) return; const res = await deleteChartUsingPOST({ id: item.id }); if (res.data) { message.success('删除成功'); load(); } }}><Button type="link" danger>删除</Button></Popconfirm>]}><List.Item.Meta avatar={<Avatar src={initialState?.currentUser?.userAvatar} />} title={item.name || '未命名分析'} description={item.chartType ? `图表类型：${item.chartType}` : '自动识别图表类型'} />{item.status === 'wait' && <Result status="warning" title="待生成" subTitle={item.execMessage || '当前任务排队中'} />}{item.status === 'running' && <Result status="info" title="图表生成中" subTitle={item.execMessage} />}{item.status === 'failed' && <Result status="error" title="图表生成失败" subTitle={item.execMessage} />}{item.status === 'succeed' && <><Tag color="success">已完成</Tag><p>分析目标：{item.goal}</p><ReactECharts option={item.genChart ? { ...JSON.parse(item.genChart), title: undefined } : {}} /></>}</Card></List.Item>} />
    <Modal open={!!selected} title={selected?.name || '分析详情'} footer={null} width={720} onCancel={() => setSelected(undefined)}><Typography.Paragraph><Typography.Text strong>分析目标：</Typography.Text>{selected?.goal || '暂无'}</Typography.Paragraph><Typography.Paragraph><Typography.Text strong>分析结论：</Typography.Text>{selected?.genResult || selected?.execMessage || '暂无'}</Typography.Paragraph></Modal></div>;
}; export default MyChartPage;
