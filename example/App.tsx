import {
  Alert,
  Button,
  SafeAreaView,
  ScrollView,
  Text,
  View,
} from "react-native";
import ReactNativeZebraLinkOs from "react-native-zebra-link-os";

// Please replace these with your actual ZPL data and variables
const TICKET_TEMPLATE = ``;
const TICKET_VARIABLES = {};

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Module API Example</Text>
        <Group name="Async functions">
          <Button
            title="Scan USB"
            onPress={async () => {
              try {
                const result = await ReactNativeZebraLinkOs.usbFindAndConnect();
                console.log("Scan USB result:", result);
              } catch (error) {
                if (error instanceof Error) {
                  Alert.alert(error.message, error.name);
                  console.error(error);
                }
              }
            }}
          />
          <Button
            title="Download Template"
            onPress={async () => {
              try {
                const result = await ReactNativeZebraLinkOs.downloadTemplate(
                  btoa(TICKET_TEMPLATE),
                );
                console.log("Download Template Result:", result);
              } catch (error) {
                if (error instanceof Error) {
                  Alert.alert(error.message, error.name);
                  console.error(error);
                }
              }
            }}
          />
          <Button
            title="Print With Downloaded Template"
            onPress={async () => {
              try {
                const result = await ReactNativeZebraLinkOs.printStoredFormat(
                  "E:TICKET.ZPL",
                  TICKET_VARIABLES,
                );
                console.log("Print Stored Format Result:", result);
              } catch (error) {
                if (error instanceof Error) {
                  Alert.alert(error.message, error.name);
                  console.error(error);
                }
              }
            }}
          />
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  view: {
    flex: 1,
    height: 200,
  },
};
