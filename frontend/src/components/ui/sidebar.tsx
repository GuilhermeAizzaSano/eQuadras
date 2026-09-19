import React, { useState } from "react";
import { cn } from "@/lib/utils";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { motion, type Transition } from "framer-motion";

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

const transitionProps: Transition = {
  type: "tween",
  ease: "easeOut",
  duration: 0.2,
};

export const SidebarItemText: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className,
}) => {
  const { isCollapsed } = useSidebar();
  if (isCollapsed) return null;

  return (
    <motion.span
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -10 }}
      transition={{ duration: 0.15 }}
      className={cn("truncate", className)}
    >
      {children}
    </motion.span>
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
        <motion.aside
          ref={ref}
          initial={isCollapsed ? "closed" : "open"}
          animate={isCollapsed ? "closed" : "open"}
          variants={{
            open: { width: expandedWidth },
            closed: { width: collapsedWidth },
          }}
          transition={transitionProps}
          onMouseEnter={() => handleHover(false)}
          onMouseLeave={() => handleHover(true)}
          className={cn(
            "fixed left-0 top-0 z-40 h-full shrink-0 border-r border-white/[0.08] bg-black select-none flex flex-col justify-between overflow-hidden",
            className
          )}
          {...(props as any)}
        >
          {header && <div className="shrink-0 border-b border-white/[0.08]">{header}</div>}

          <div className="flex-1 min-h-0 overflow-hidden">
            <ScrollArea className="h-full p-2">{children}</ScrollArea>
          </div>

          {footer && <div className="shrink-0 border-t border-white/[0.08] p-2">{footer}</div>}
        </motion.aside>
      </SidebarContext.Provider>
    );
  }
);
Sidebar.displayName = "Sidebar";

export { Separator };
