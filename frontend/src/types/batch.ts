export interface BatchSchedule {
  jobName: string;
  enabled: boolean;
  fixedDelaySeconds: number;
  running: boolean;
  lastStartedAt: string | null;
  lastEndedAt: string | null;
  lastStatus: 'SUCCESS' | 'FAIL' | 'SKIPPED' | null;
  lastMessage: string | null;
}

export interface BatchRunResult {
  triggered: boolean;
  status: 'STARTED' | 'SKIPPED';
}

export interface BatchScheduleUpdateBody {
  enabled: boolean;
  fixedDelaySeconds: number;
  modifiedBy: string;
}
