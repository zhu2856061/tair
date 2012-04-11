/*
 * (C) 2007-2010 Alibaba Group Holding Limited
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * some commonly used util class
 *
 * Version: $Id$
 *
 * Authors:
 *   ruohai <ruohai@taobao.com>
 *     - initial release
 *
 */
#include "util.hpp"
namespace tair {
  namespace util {
    uint64_t local_server_ip::ip;

    int file_util::change_conf(const char *group_file_name, const char *section_name, const char *key, const char *value) {
      FILE *fd = fopen(group_file_name, "r+");
      if (fd == NULL) {
        log_error("open group file %s failed: %m", group_file_name);
        return TAIR_RETURN_FAILED;
      }
      if (key == NULL) {
        return TAIR_RETURN_FAILED;
      }
      if (value == NULL) {
        value = "";
      }

      char section[128];
      snprintf(section, sizeof(section), "[%s]", section_name);

      vector<string> lines;
      lines.reserve(100);
      char line[1024]; //! assuming one line has less than 1k chars
      while (fgets(line, sizeof(line), fd) != NULL) {
        lines.push_back(line);
      }
      fclose(fd);

      char key_value[64];
      snprintf(key_value, sizeof(key_value), "%s=%s\n", key, value);
      //~ searching for target section
      size_t i = 0;
      for (; i < lines.size(); ++i) {
        const char *p = lines[i].c_str();
        while (*p && (*p == ' ' || *p == '\t')) ++p;
        if (*p == '#')
          continue;
        if (strstr(p, section) == NULL) {
          continue;
        }
        break;
      }
      if (i == lines.size()) {
        return TAIR_RETURN_FAILED; //~ no such section
      }
      ++i;
      //~ searching for boundary of the next section
      size_t j = i;
      for (; j < lines.size(); ++j) {
        const char *p = lines[j].c_str();
        while (*p && (*p == ' ' || *p == '\t')) ++p;
        if (*p == '#')
          continue;
        if (*p == '[')
          break;
      }
      //~ searching for the specific _group_status
      size_t k = i;
      for (; k < j; ++k) {
        const char *p = lines[k].c_str();
        while (*p && (*p == ' ' || *p == '\t')) ++p;
        if (*p == '#')
          continue;
        if (strstr(p, TAIR_GROUP_STATUS) != NULL) {
          lines[k] = key_value;
          break;
        }
      }
      if (k == j) {
        lines.insert(lines.begin() + i, key_value);
      }

      //~ write to temp file, then rename it
      char tmpfile[128];
      snprintf(tmpfile, sizeof(tmpfile), "%s.%d", group_file_name, getpid());
      fd = fopen(tmpfile, "w+");
      if (fd == NULL) {
        log_error("open temp file %s failed, %m", tmpfile);
        return TAIR_RETURN_FAILED;
      }
      for (i = 0; i < lines.size(); ++i) {
        fprintf(fd, lines[i].c_str());
      }
      fclose(fd);
      if (0 == rename(tmpfile, group_file_name)) {
        return TAIR_RETURN_SUCCESS;
      } else {
        return TAIR_RETURN_FAILED;
      }
    }
  }
}
