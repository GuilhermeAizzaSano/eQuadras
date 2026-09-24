import React, { useState } from "react";
import { cn } from "@/lib/utils";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";

export interface SidebarContextValue {
  isCollapsed: boolean;
  setIsCollapsed: (collapsed: boolean) => void;
}

const SidebarContext = React.createContext<SidebarContextValue | undefined>(undefined);

export function useSidebar() {
  const context = React.useContext(SidebarContext);
  if (!context) {
    throw new Error("useSidebar must be used within a SidebarProvider");
  }
  return context;
}

export interface SidebarProps extends React.HTMLAttributes<HTMLDivElement> {
  defaultCollapsed?: boolean;
  isCollapsed?: boolean;
  onCollapseChange?: (collapsed: boolean) => void;
  collapsedWidth?: string;
  expandedWidth?: string;
  header?: React.ReactNode;
  footer?: React.ReactNode;
}

export const SidebarItemText: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className,
}) => {
  const { isCollapsed } = useSidebar();
  if (isCollapsed) return null;

  return (
    <span
      className={cn(
        "truncate animate-in fade-in duration-150 transition-opacity",
        className
      )}
    >
      {children}
    </span>
  );
};

export const Sidebar = React.forwardRef<HTMLDivElement, SidebarProps>(
  (
    {
      className,
      children,
      defaultCollapsed = true,
      isCollapsed: controlledCollapsed,
      onCollapseChange,
      collapsedWidth = "3.25rem",
      expandedWidth = "15rem",
      header,
      footer,
      style,
      ...props
    },
    ref
  ) => {
    const [internalCollapsed, setInternalCollapsed] = useState(defaultCollapsed);
    const isCollapsed = controlledCollapsed !== undefined ? controlledCollapsed : internalCollapsed;

    const handleHover = (collapsed: boolean) => {
      setInternalCollapsed(collapsed);
      onCollapseChange?.(collapsed);
    };

    return (
      <SidebarContext.Provider value={{ isCollapsed, setIsCollapsed: setInternalCollapsed }}>
        <aside
          ref={ref}
          onMouseEnter={() => handleHover(false)}
          onMouseLeave={() => handleHover(true)}
          style={{
            width: isCollapsed ? collapsedWidth : expandedWidth,
            ...style,
          }}
          className={cn(
            "fixed left-0 top-0 z-40 h-full shrink-0 border-r border-fg/[0.1] bg-bg select-none flex flex-col justify-between overflow-hidden transition-[width] duration-200 ease-out",
            className
          )}
          {...props}
        >
          {header && <div className="shrink-0 border-b border-fg/[0.1]">{header}</div>}

          <div className="flex-1 min-h-0 overflow-hidden">
            <ScrollArea className="h-full p-2">{children}</ScrollArea>
          </div>

          {footer && <div className="shrink-0 border-t border-fg/[0.1] p-2">{footer}</div>}
        </aside>
      </SidebarContext.Provider>
    );
  }
);
Sidebar.displayName = "Sidebar";

export { Separator };
