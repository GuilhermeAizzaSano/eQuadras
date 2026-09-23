export interface PageResponse<T> {
  content: T[];
  page: number;          // 0-based (Spring)
  size: number;
  totalElements: number;
  totalPages: number;
}
