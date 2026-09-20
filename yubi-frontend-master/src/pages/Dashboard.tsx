import { listMyChartByPageUsingPOST } from '@/services/yubi/chartController';
import { PageContainer, StatisticCard } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { Button, Card, Col, Empty, Row, Space, Tag, Typography } from 'antd';
import React, { useEffect, useState } from 'react';

const Dashboard: React.FC = () => {
  const [charts, setCharts] = useState<API.Chart[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listMyChartByPageUsingPOST({ current: 1, pageSize: 8, sortField: 'createTime', sortOrder: 'desc' })
      .then((res) => setCharts(res.data?.records ?? []))
      .finally(() => setLoading(false));
  }, []);

  const succeed = charts.filter((item) => item.status === 'succeed').length;
  const running = charts.filter((item) => item.status === 'wait' || item.status === 'running').length;
  const failed = charts.filter((item) => item.status === 'failed').length;

  return (
    <PageContainer title="分析工作台" subTitle="上传数据，使用 AI 快速生成可视化分析">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}><StatisticCard statistic={{ title: '分析总数', value: charts.length }} loading={loading} /></Col>
        <Col xs={24} sm={12} lg={6}><StatisticCard statistic={{ title: '已完成', value: succeed, suffix: '个' }} loading={loading} /></Col>
        <Col xs={24} sm={12} lg={6}><StatisticCard statistic={{ title: '处理中', value: running, suffix: '个' }} loading={loading} /></Col>
        <Col xs={24} sm={12} lg={6}><StatisticCard statistic={{ title: '失败任务', value: failed, suffix: '个' }} loading={loading} /></Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="最近分析" extra={<Button type="link" onClick={() => history.push('/my_chart')}>查看全部</Button>}>
            {charts.length === 0 ? <Empty description="还没有分析记录，去创建第一张图表吧" /> : charts.slice(0, 5).map((item) => (
              <Card.Grid key={item.id} style={{ width: '100%' }} hoverable onClick={() => history.push('/my_chart')}>
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{item.name || '未命名分析'}</Typography.Text>
                  <Typography.Text type="secondary">{item.goal || '暂无分析目标'}</Typography.Text>
                  <Tag color={item.status === 'succeed' ? 'success' : item.status === 'failed' ? 'error' : 'processing'}>{item.status}</Tag>
                </Space>
              </Card.Grid>
            ))}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="快捷操作">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" block onClick={() => history.push('/add_chart')}>创建智能分析</Button>
              <Button block onClick={() => history.push('/add_chart_async')}>创建异步分析</Button>
              <Button block onClick={() => history.push('/my_chart')}>管理我的图表</Button>
            </Space>
          </Card>
          <Card title="平台能力" style={{ marginTop: 16 }}>
            <Typography.Paragraph>支持 Excel/CSV 数据上传、AI 结论生成、ECharts 可视化、RabbitMQ 异步任务和 Redis 限流。</Typography.Paragraph>
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default Dashboard;
